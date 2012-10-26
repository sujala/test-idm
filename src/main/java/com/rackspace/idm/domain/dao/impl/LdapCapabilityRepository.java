package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.entity.Capabilities;
import com.rackspace.idm.domain.entity.Capability;
import org.springframework.stereotype.Component;
import org.apache.commons.lang.StringUtils;
import com.unboundid.ldap.sdk.*;
import com.unboundid.ldap.sdk.persist.LDAPPersistException;
import com.unboundid.ldap.sdk.persist.LDAPPersister;

import java.util.Arrays;
import java.util.List;


/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 10/22/12
 * Time: 4:43 PM
 * To change this template use File | Settings | File Templates.
 */
@Component
public class LdapCapabilityRepository extends LdapRepository implements CapabilityDao {

    public static final String NULL_OR_EMPTY_CAPABILITY_ID_PARAMETER = "Null or Empty capabilityId parameter";
    public static final String ERROR_GETTING_CAPABILITY_OBJECT = "Error getting capability object";

    @Override
    public Capability getCapability(String capabilityId, String endpointTemplateId) {
        getLogger().debug("Doing search for Capability " + capabilityId);
        if (StringUtils.isBlank(capabilityId)) {
            getLogger().error(NULL_OR_EMPTY_CAPABILITY_ID_PARAMETER);
            getLogger().info("Invalid Capability parameter.");
            return null;
        }

        Filter searchFilter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_ID, capabilityId)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_CAPABILITY).build();

        Capability capability;

        try {
            capability = getSingleCapability(searchFilter, endpointTemplateId);
        } catch (LDAPPersistException e) {
            getLogger().error(ERROR_GETTING_CAPABILITY_OBJECT, e);
            throw new IllegalStateException(e);
        }
        getLogger().debug("Found capability - {}", capability);

        return capability;
    }

    @Override
    public Capabilities getCapabilities(String endpointTemplateId) {
        getLogger().debug("Getting Capabilities");

        Capabilities capabilities = new Capabilities();
        SearchResult searchResult;

        Filter searchFilter = new LdapSearchBuilder().addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_CAPABILITY).build();

        try {
            String baseDn = String.format("rsId=%s,ou=baseUrls,ou=cloud,o=rackspace,dc=rackspace,dc=com", endpointTemplateId);
            searchResult = getAppInterface().search(baseDn, SearchScope.ONE, searchFilter);
            getLogger().info("Got Capabilities");
        } catch (LDAPSearchException ldapEx) {
            getLogger().error("Error searching for Capabilities - {}", ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (searchResult.getEntryCount() > 0) {
            for (SearchResultEntry entry : searchResult.getSearchEntries()) {
                capabilities.getCapability().add(getEntryCapability(entry));
            }
        }

        return capabilities;
    }

    @Override
    public void updateCapabilities(String endpointTemplateId, Capabilities capabilities) {
        getLogger().debug("Replacing old capabilities with {}", capabilities);
        getLogger().debug("Removing old capabilities");

        if (StringUtils.isBlank(endpointTemplateId)) {
            getLogger().error(NULL_OR_EMPTY_CAPABILITY_ID_PARAMETER);
            throw new IllegalArgumentException(NULL_OR_EMPTY_CAPABILITY_ID_PARAMETER);
        }
        //Removing old capabilities
        removeCapabilities(endpointTemplateId);

        //Adding new capabilities
        for (Capability newCap : capabilities.getCapability()) {
            if (newCap == null) {
                getLogger().error(ERROR_GETTING_CAPABILITY_OBJECT);
                throw new IllegalArgumentException(ERROR_GETTING_CAPABILITY_OBJECT);
            }
            if(newCap.getResources() == null || newCap.getResources().size() <= 0){
                newCap.setResources(null);
            }
            getLogger().info("Adding capability: {}", newCap);
            Audit audit = Audit.log(newCap).add();
            try {
                final LDAPPersister<Capability> persister = LDAPPersister.getInstance(Capability.class);
                String baseDn = String.format("rsId=%s,ou=baseUrls,ou=cloud,o=rackspace,dc=rackspace,dc=com", endpointTemplateId);
                persister.add(newCap, getAppInterface(), baseDn);
                audit.succeed();
                getLogger().info("Added capability: {}", newCap);
            } catch (final LDAPException e) {
                getLogger().error("Error adding capability object", e);
                audit.fail(e.getMessage());
                throw new IllegalStateException(e);
            }
        }
    }

    @Override
    public void removeCapabilities(String endpointTemplateId) {
        if (StringUtils.isBlank(endpointTemplateId)) {
            getLogger().error(NULL_OR_EMPTY_CAPABILITY_ID_PARAMETER);
            throw new IllegalArgumentException(NULL_OR_EMPTY_CAPABILITY_ID_PARAMETER);
        }

        Capabilities capabilities = getCapabilities(endpointTemplateId);

        if(capabilities.getCapability().size() <= 0){
            return;
        }

        for (Capability capability : capabilities.getCapability()) {
            getLogger().debug("Deleting capability: {}", capability);
            final String dn = String.format("rsId=%s,rsId=%s,ou=baseUrls,ou=cloud,o=rackspace,dc=rackspace,dc=com", capability.getCapabilityId(), endpointTemplateId);
            final Audit audit = Audit.log(capability).delete();
            deleteEntryAndSubtree(dn, audit);
            audit.succeed();
            getLogger().debug("Deleted capability: {}", capability);
        }
    }

    Capability getEntryCapability(SearchResultEntry entry) {
        getLogger().debug("Inside getEntryCapability");
        Capability capability = new Capability();
        capability.setAction(entry.getAttributeValue(ATTR_ACTION));
        capability.setCapabilityId(entry.getAttributeValue(ATTR_ID));
        capability.setName(entry.getAttributeValue(ATTR_NAME));
        capability.setUrl(entry.getAttributeValue(ATTR_URL));
        if (entry.getAttributeValue(ATTR_DESCRIPTION) != null) {
            capability.setDescription(entry.getAttributeValue(ATTR_DESCRIPTION));
        }
        if (entry.getAttributeValues(ATTR_RESOURCES) != null) {
            capability.setResources(Arrays.asList(entry.getAttributeValues(ATTR_RESOURCES)));
        }
        return capability;
    }

    Capability getSingleCapability(Filter searchFilter, String endpointTemplateId)
            throws LDAPPersistException {
        String baseDn = String.format("rsId=%s,ou=baseUrls,ou=cloud,o=rackspace,dc=rackspace,dc=com", endpointTemplateId);
        SearchResultEntry entry = this.getSingleEntry(baseDn, SearchScope.ONE, searchFilter, ATTR_CAPABILITY_SEARCH_ATTRIBUTES);
        if (entry == null) {
            return null;
        }
        Capability capability = LDAPPersister.getInstance(Capability.class).decode(entry);
        return capability;
    }
}
