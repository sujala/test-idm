package com.rackspace.idm.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import com.rackspace.idm.entities.CloudBaseUrl;
import com.rackspace.idm.entities.CloudEndpoint;
import com.rackspace.idm.entities.EndPoints;
import com.rackspace.idm.exceptions.NotFoundException;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPResult;
import com.unboundid.ldap.sdk.LDAPSearchException;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;

public class LdapEndpointRepository extends LdapRepository implements
    EndpointDao {

    public LdapEndpointRepository(LdapConnectionPools connPools,
        Configuration config, Logger logger) {
        super(connPools, config, logger);
    }

    public void addBaseUrl(CloudBaseUrl baseUrl) {
        getLogger().info("Adding baseUlr {}", baseUrl);
        if (baseUrl == null) {
            getLogger().error("Null instance of baseUrl was passed");
            throw new IllegalArgumentException(
                "Null instance of baseUrl was passed.");
        }

        List<Attribute> atts = new ArrayList<Attribute>();

        atts.add(new Attribute(ATTR_OBJECT_CLASS,
            ATTR_BASEURL_OBJECT_CLASS_VALUES));

        if (!StringUtils.isBlank(baseUrl.getAdminUrl())) {
            atts.add(new Attribute(ATTR_ADMIN_URL, baseUrl.getAdminUrl()));
        }

        if (!StringUtils.isBlank(baseUrl.getBaseUrlType())) {
            atts.add(new Attribute(ATTR_BASEURL_TYPE, baseUrl.getBaseUrlType()));
        }

        if (baseUrl.getBaseUrlId() != null
            && baseUrl.getBaseUrlId().intValue() > 0) {
            atts.add(new Attribute(ATTR_BASEURL_ID, baseUrl.getBaseUrlId()
                .toString()));
        }

        if (!StringUtils.isBlank(baseUrl.getInternalUrl())) {
            atts.add(new Attribute(ATTR_INTERNAL_URL, baseUrl.getInternalUrl()));
        }

        if (!StringUtils.isBlank(baseUrl.getPublicUrl())) {
            atts.add(new Attribute(ATTR_PUBLIC_URL, baseUrl.getPublicUrl()));
        }

        if (!StringUtils.isBlank(baseUrl.getRegion())) {
            atts.add(new Attribute(ATTR_REGION, baseUrl.getRegion()));
        }

        if (!StringUtils.isBlank(baseUrl.getService())) {
            atts.add(new Attribute(ATTR_SERVICE, baseUrl.getService()));
        }

        if (baseUrl.getDef() != null) {
            atts.add(new Attribute(ATTR_DEF, baseUrl.getDef().toString()));
        }

        String baseUrlDN = new LdapDnBuilder(BASEURL_BASE_DN).addAttriubte(
            ATTR_BASEURL_ID, String.valueOf(baseUrl.getBaseUrlId())).build();

        LDAPResult result;

        try {
            result = getAppConnPool().add(baseUrlDN, atts);
        } catch (LDAPException ldapEx) {
            getLogger().error("Error adding baseUrl {} - {}", baseUrl, ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            getLogger().error("Error adding baseUrl {} - {}",
                baseUrl.getBaseUrlId(), result.getResultCode());
            throw new IllegalStateException(String.format(
                "LDAP error encountered when adding baseUrl: %s - %s",
                baseUrl.getBaseUrlId(), result.getResultCode().toString()));
        }

        getLogger().debug("Added baseUrl {}", baseUrl);
    }

    public void addBaseUrlToUser(int baseUrlId, boolean def, String username) {
        CloudBaseUrl baseUrl = this.getBaseUrlById(baseUrlId);
        if (baseUrl == null) {
            String errMsg = String.format("BaseUrlId %s not found", baseUrlId);
            getLogger().error(errMsg);
            throw new NotFoundException(errMsg);
        }

        String newEndpoint = def ? "+" : "-";
        newEndpoint = newEndpoint + String.valueOf(baseUrlId);

        EndPoints oldEndpoints = this.getRawEndpointsForUser(username);

        List<String> endpoints = new ArrayList<String>();

        for (String s : oldEndpoints.getEndpoints()) {
            if (s.equals(newEndpoint)) {
                return;
            }
            endpoints.add(s);
        }

        endpoints.add(newEndpoint);

        List<Modification> mods = new ArrayList<Modification>();

        String[] points = endpoints.toArray(new String[endpoints.size()]);

        mods.add(new Modification(ModificationType.REPLACE, ATTR_ENDPOINT,
            points));

        LDAPResult result = null;
        try {
            result = getAppConnPool().modify(oldEndpoints.getUserDN(), mods);
        } catch (LDAPException ldapEx) {
            getLogger().error("Error updating user {} endpoints - {}",
                username, ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            getLogger().error("Error updating user {} endpoints - {}",
                username, result.getResultCode());
            throw new IllegalArgumentException(String.format(
                "LDAP error encountered when updating user %s endpoints - %s",
                username, result.getResultCode().toString()));
        }
    }

    public void deleteBaseUrl(int baseUrlId) {
        getLogger().info("Deleting baseUrl - {}", baseUrlId);

        LDAPResult result = null;

        String baseUrlDN = new LdapDnBuilder(BASEURL_BASE_DN).addAttriubte(
            ATTR_BASEURL_ID, String.valueOf(baseUrlId)).build();

        try {
            result = getAppConnPool().delete(
                String.format(baseUrlDN, baseUrlId));
        } catch (LDAPException ldapEx) {
            getLogger().error("Error deleting baseUlr {} - {}", baseUrlId,
                ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            getLogger().error("Error deleting baseUrl {} - {}", baseUrlId,
                result.getResultCode());
            throw new IllegalStateException(String.format(
                "LDAP error encountered when deleting baseUrl: %s - %s",
                baseUrlId, result.getResultCode().toString()));
        }

        getLogger().info("Deleted baseUrl - {}", baseUrlId);
    }

    public CloudBaseUrl getBaseUrlById(int baseUrlId) {
        CloudBaseUrl baseUrl = null;
        SearchResult searchResult = null;

        String searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_BASEURL_ID, String.valueOf(baseUrlId))
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_BASEURL).build();

        try {
            searchResult = getAppConnPool().search(BASEURL_BASE_DN,
                SearchScope.ONE, searchFilter);
        } catch (LDAPSearchException ldapEx) {
            getLogger().error("Error searching for baseUrl - {}", ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (searchResult.getEntryCount() == 1) {
            SearchResultEntry e = searchResult.getSearchEntries().get(0);
            baseUrl = getBaseUrl(e);
        } else if (searchResult.getEntryCount() > 1) {
            String errMsg = String.format(
                "More than one entry was found for baseUrl - %s", baseUrlId);
            getLogger().error(errMsg);
            throw new IllegalStateException(errMsg);
        }

        return baseUrl;
    }

    public List<CloudBaseUrl> getBaseUrls() {

        List<CloudBaseUrl> baseUrls = new ArrayList<CloudBaseUrl>();
        SearchResult searchResult = null;

        String searchFilter = new LdapSearchBuilder().addEqualAttribute(
            ATTR_OBJECT_CLASS, OBJECTCLASS_BASEURL).build();

        try {
            searchResult = getAppConnPool().search(BASEURL_BASE_DN,
                SearchScope.ONE, searchFilter);
        } catch (LDAPSearchException ldapEx) {
            getLogger().error("Error searching for baseUrls - {}", ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (searchResult.getEntryCount() > 0) {
            for (SearchResultEntry entry : searchResult.getSearchEntries()) {
                baseUrls.add(getBaseUrl(entry));
            }
        }
        return baseUrls;
    }

    public List<CloudEndpoint> getEndpointsForUser(String username) {
        EndPoints points = this.getRawEndpointsForUser(username);

        if (points == null || points.getEndpoints().size() < 1) {
            return null;
        }

        List<CloudEndpoint> endpoints = new ArrayList<CloudEndpoint>();

        for (String endpoint : points.getEndpoints()) {
            boolean def = endpoint.startsWith("+");
            int baseUrlId = Integer.parseInt(endpoint.substring(1));
            CloudBaseUrl baseUrl = this.getBaseUrlById(baseUrlId);
            if (baseUrl != null) {
                CloudEndpoint point = new CloudEndpoint();
                point.setBaseUrl(baseUrl);
                point.setMossoId(points.getMossoId());
                point.setUsername(points.getUsername());
                point.setNastId(points.getNastId());
                point.setV1preferred(def);
                endpoints.add(point);
            }
        }
        return endpoints;
    }

    public void removeBaseUrlFromUser(int baseUrlId, String username) {
        CloudBaseUrl baseUrl = this.getBaseUrlById(baseUrlId);
        if (baseUrl == null) {
            String errMsg = String.format("BaseUrlId %s not found", baseUrlId);
            getLogger().error(errMsg);
            throw new NotFoundException(errMsg);
        }

        EndPoints endpoints = this.getRawEndpointsForUser(username);

        if (endpoints.getEndpoints().size() < 1) {
            return;
        }

        List<String> newEndpoints = new ArrayList<String>();

        for (String endpoint : endpoints.getEndpoints()) {
            if (!endpoint.substring(1).equals(String.valueOf(baseUrlId))) {
                newEndpoints.add(endpoint);
            }
        }

        List<Modification> mods = new ArrayList<Modification>();

        if (newEndpoints.size() < 1) {
            // If a user's last endpoint has been removed we need to delete
            // the attriubte from LDAP
            mods.add(new Modification(ModificationType.DELETE, ATTR_ENDPOINT));
        } else {
            // Else we'll just replace all the values for endpoints with the
            // reduced list.
            String[] points = newEndpoints.toArray(new String[newEndpoints
                .size()]);
            mods.add(new Modification(ModificationType.REPLACE, ATTR_ENDPOINT,
                points));
        }

        LDAPResult result = null;
        try {
            result = getAppConnPool().modify(endpoints.getUserDN(), mods);
        } catch (LDAPException ldapEx) {
            getLogger().error("Error updating user {} endpoints - {}",
                username, ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            getLogger().error("Error updating user {} endpoints - {}",
                username, result.getResultCode());
            throw new IllegalArgumentException(String.format(
                "LDAP error encountered when updating user %s endpoints - %s",
                username, result.getResultCode().toString()));
        }
    }

    private CloudBaseUrl getBaseUrl(SearchResultEntry resultEntry) {
        CloudBaseUrl baseUrl = new CloudBaseUrl();
        baseUrl.setAdminUrl(resultEntry.getAttributeValue(ATTR_ADMIN_URL));
        baseUrl.setBaseUrlId(resultEntry
            .getAttributeValueAsInteger(ATTR_BASEURL_ID));
        baseUrl
            .setBaseUrlType(resultEntry.getAttributeValue(ATTR_BASEURL_TYPE));
        baseUrl.setDef(resultEntry.getAttributeValueAsBoolean(ATTR_DEF));
        baseUrl
            .setInternalUrl(resultEntry.getAttributeValue(ATTR_INTERNAL_URL));
        baseUrl.setPublicUrl(resultEntry.getAttributeValue(ATTR_PUBLIC_URL));
        baseUrl.setRegion(resultEntry.getAttributeValue(ATTR_REGION));
        baseUrl.setService(resultEntry.getAttributeValue(ATTR_SERVICE));
        return baseUrl;
    }

    private EndPoints getRawEndpointsForUser(String username) {
        List<String> userEndpoints = new ArrayList<String>();
        String userDN = null;
        String nastId = null;
        Integer mossoId = null;
        SearchResult searchResult = null;

        String searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_UID, username)
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON)
            .build();

        try {
            searchResult = getAppConnPool().search(
                BASE_DN,
                SearchScope.SUB,
                searchFilter,
                new String[]{ATTR_ENDPOINT, ATTR_UID, ATTR_NAST_ID,
                    ATTR_MOSSO_ID});
        } catch (LDAPSearchException ldapEx) {
            getLogger().error("Error searching for baseUrls - {}", ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (searchResult.getEntryCount() == 0) {
            String errMsg = String.format("User %s not found.", username);
            getLogger().error(errMsg);
            throw new NotFoundException(errMsg);
        } else if (searchResult.getEntryCount() == 1) {
            SearchResultEntry e = searchResult.getSearchEntries().get(0);
            String[] list = e.getAttributeValues(ATTR_ENDPOINT);
            if (list != null && list.length > 0) {
                userEndpoints = Arrays.asList(list);
            }
            userDN = e.getDN();
            nastId = e.getAttributeValue(ATTR_NAST_ID);
            mossoId = e.getAttributeValueAsInteger(ATTR_MOSSO_ID);
        } else if (searchResult.getEntryCount() > 1) {
            String errMsg = String.format(
                "More than one entry was found for user - %s", username);
            getLogger().error(errMsg);
            throw new IllegalStateException(errMsg);
        }

        EndPoints points = new EndPoints();
        points.setMossoId(mossoId);
        points.setUsername(username);
        points.setNastId(nastId);
        points.setEndpoints(userEndpoints);
        points.setUserDN(userDN);
        return points;
    }
}
