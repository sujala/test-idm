package com.rackspace.idm.domain.dao.impl;

import org.springframework.stereotype.Component;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.dao.EndpointDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.exception.BaseUrlConflictException;
import com.rackspace.idm.exception.NotFoundException;
import com.unboundid.ldap.sdk.*;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class LdapEndpointRepository extends LdapRepository implements EndpointDao {

    @Override
    public void addBaseUrl(CloudBaseUrl baseUrl) {

        if (baseUrl == null) {
            getLogger().error("Null instance of baseUrl was passed");
            throw new IllegalArgumentException(
                    "Null instance of baseUrl was passed.");
        }

        getLogger().debug("Adding baseUlr {}", baseUrl);

        List<Attribute> atts = new ArrayList<Attribute>();

        atts.add(new Attribute(ATTR_OBJECT_CLASS, ATTR_BASEURL_OBJECT_CLASS_VALUES));

        if (!StringUtils.isBlank(baseUrl.getAdminUrl())) {
            atts.add(new Attribute(ATTR_ADMIN_URL, baseUrl.getAdminUrl()));
        }

        if (!StringUtils.isBlank(baseUrl.getBaseUrlType())) {
            atts.add(new Attribute(ATTR_BASEURL_TYPE, baseUrl.getBaseUrlType()));
        }

        if (baseUrl.getBaseUrlId() != null && baseUrl.getBaseUrlId().intValue() > 0) {
            atts.add(new Attribute(ATTR_ID, baseUrl.getBaseUrlId().toString()));
        }

        if (!StringUtils.isBlank(baseUrl.getInternalUrl())) {
            atts.add(new Attribute(ATTR_INTERNAL_URL, baseUrl.getInternalUrl()));
        }

        if (!StringUtils.isBlank(baseUrl.getPublicUrl())) {
            atts.add(new Attribute(ATTR_PUBLIC_URL, baseUrl.getPublicUrl()));
        }

        // Renamed to ServiceName below since both were sharing
        //if (!StringUtils.isBlank(baseUrl.getName())) {
        //    atts.add(new Attribute(ATTR_NAME, baseUrl.getName()));
        //}

        if (!StringUtils.isBlank(baseUrl.getRegion())) {
            atts.add(new Attribute(ATTR_REGION, baseUrl.getRegion()));
        }

        // Renamed to ServiceName since both above Name were sharing with Service
        if (!StringUtils.isBlank(baseUrl.getServiceName())) {
            atts.add(new Attribute(ATTR_SERVICE, baseUrl.getServiceName()));
        }

        if (baseUrl.getDef() != null) {
            atts.add(new Attribute(ATTR_DEF, baseUrl.getDef().toString()));
        }

        if (baseUrl.getEnabled() != null) {
            atts.add(new Attribute(ATTR_ENABLED, baseUrl.getEnabled().toString()));
        }

        if (!StringUtils.isBlank(baseUrl.getOpenstackType())) {
            atts.add(new Attribute(ATTR_OPENSTACK_TYPE, baseUrl.getOpenstackType()));
        }

        if (!StringUtils.isBlank(baseUrl.getVersionId())) {
            atts.add(new Attribute(ATTR_VERSION_ID, baseUrl.getVersionId()));
        }

        if (!StringUtils.isBlank(baseUrl.getVersionInfo())) {
            atts.add(new Attribute(ATTR_VERSION_INFO, baseUrl.getVersionInfo()));
        }

        if (!StringUtils.isBlank(baseUrl.getVersionList())) {
            atts.add(new Attribute(ATTR_VERSION_LIST, baseUrl.getVersionList()));
        }

        if (baseUrl.getGlobal() != null) {
            atts.add(new Attribute(ATTR_GLOBAL, baseUrl.getGlobal().toString()));
        }


        String baseUrlDN = new LdapDnBuilder(BASEURL_BASE_DN).addAttribute(ATTR_ID, String.valueOf(baseUrl.getBaseUrlId())).build();

        LDAPResult result;

        try {
            result = getAppInterface().add(baseUrlDN, atts);
            getLogger().info("Added basedUrl {}", baseUrl);
        } catch (LDAPException ldapEx) {
            getLogger().error("Error adding baseUrl {} - {}", baseUrl, ldapEx);
            throw new IllegalStateException(ldapEx.getMessage(), ldapEx);
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

    @Override
    public void addBaseUrlToUser(int baseUrlId, boolean def, String username) {
        getLogger().debug("Adding baseUlr {} to user {}", baseUrlId, username);
        CloudBaseUrl baseUrl = this.getBaseUrlById(baseUrlId);
        if (baseUrl == null) {
            String errMsg = String.format("BaseUrlId %s not found", baseUrlId);
            getLogger().error(errMsg);
            throw new NotFoundException(errMsg);
        }

        String newEndpoint = def ? "+" : "-";
        newEndpoint = newEndpoint + baseUrlId;

        EndPoints oldEndpoints = this.getRawEndpointsForUser(username);

        List<String> endpoints = new ArrayList<String>();

        for (String s : oldEndpoints.getEndpoints()) {
            if (s.equals("-" + baseUrlId) || s.equals("+" + baseUrlId)) {
                throw new BaseUrlConflictException("Attempt to add existing BaseURL!");
            }
            endpoints.add(s);
        }

        endpoints.add(newEndpoint);

        List<Modification> mods = new ArrayList<Modification>();

        String[] points = endpoints.toArray(new String[endpoints.size()]);

        mods.add(new Modification(ModificationType.REPLACE, ATTR_ENDPOINT, points));

        LDAPResult result = null;
        try {
            result = getAppInterface().modify(oldEndpoints.getUserDN(), mods);
            getLogger().debug("Added baseUlr {} to user {}", baseUrlId, username);
        } catch (LDAPException ldapEx) {
            getLogger().error("Error updating user {} endpoints - {}", username, ldapEx);
            throw new IllegalStateException(ldapEx.getMessage(), ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            getLogger().error("Error updating user {} endpoints - {}", username, result.getResultCode());
            throw new IllegalArgumentException(
                    String.format("LDAP error encountered when updating user %s endpoints - %s", username, result.getResultCode().toString()));
        }
        getLogger().debug("Adding baseUlr {} to user {}", baseUrlId, username);
    }

    @Override
    public void deleteBaseUrl(int baseUrlId) {
        getLogger().debug("Deleting baseUrl - {}", baseUrlId);

        LDAPResult result = null;

        String baseUrlDN = new LdapDnBuilder(BASEURL_BASE_DN).addAttribute(
                ATTR_ID, String.valueOf(baseUrlId)).build();

        try {
            result = getAppInterface().delete(String.format(baseUrlDN, baseUrlId));
            getLogger().info("Deleted baseUrl - {}", baseUrlId);
        } catch (LDAPException ldapEx) {
            getLogger().error("Error deleting baseUlr {} - {}", baseUrlId, ldapEx);
            throw new IllegalStateException(ldapEx.getMessage(), ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            getLogger().error("Error deleting baseUrl {} - {}", baseUrlId, result.getResultCode());
            throw new IllegalStateException(String.format(
                    "LDAP error encountered when deleting baseUrl: %s - %s",
                    baseUrlId, result.getResultCode().toString()));
        }

        getLogger().debug("Deleted baseUrl - {}", baseUrlId);
    }

    @Override
    public CloudBaseUrl getBaseUrlById(int baseUrlId) {
        getLogger().debug("Get baseurl by Id {}", baseUrlId);
        CloudBaseUrl baseUrl = null;

        Filter searchFilter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_ID, String.valueOf(baseUrlId))
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_BASEURL).build();


        SearchResultEntry searchResult = this.getSingleEntry(BASEURL_BASE_DN, SearchScope.ONE, searchFilter);
        baseUrl = getBaseUrl(searchResult);
        getLogger().debug("Got baseurl by Id {}", baseUrlId);

        return baseUrl;
    }

    @Override
    public List<CloudBaseUrl> getBaseUrlsByService(String service) {
        getLogger().debug("Getting baseUrls by service {}", service);
        List<CloudBaseUrl> cloudBaseUrls = new ArrayList<CloudBaseUrl>();
        List<SearchResultEntry> searchResultEntries = null;

        Filter searchFilter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_SERVICE, service)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_BASEURL).build();

        searchResultEntries = getMultipleEntries(BASEURL_BASE_DN, SearchScope.ONE, searchFilter);
        getLogger().info("Got baseUrls by Id {}", service);

        for (SearchResultEntry e : searchResultEntries) {
            cloudBaseUrls.add(getBaseUrl(e));
        }
        return cloudBaseUrls;
    }

    @Override
    public List<CloudBaseUrl> getBaseUrls() {

        getLogger().debug("Getting baseurls");

        List<CloudBaseUrl> baseUrls = new ArrayList<CloudBaseUrl>();
        List<SearchResultEntry> searchResultEntries = null;

        Filter searchFilter = new LdapSearchBuilder().addEqualAttribute(
                ATTR_OBJECT_CLASS, OBJECTCLASS_BASEURL).build();


        searchResultEntries = getMultipleEntries(BASEURL_BASE_DN, SearchScope.ONE, searchFilter);
        getLogger().info("Got baseurls");

        if (searchResultEntries.size() > 0) {
            for (SearchResultEntry entry : searchResultEntries) {
                baseUrls.add(getBaseUrl(entry));
            }
        }

        getLogger().debug("Got baseurls");
        return baseUrls;
    }

    @Override
    public List<CloudEndpoint> getEndpointsForUser(String username) {

        getLogger().debug("Getting Endpoints for User {}", username);

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
                getLogger().debug("Got Endpoints for User {}", username);
            }
        }
        getLogger().debug("Got Endpoints for User {}", username);
        return endpoints;
    }

    @Override
    public OpenstackEndpoint getOpenstackEndpointsForTenant(Tenant tenant) {

        List<CloudBaseUrl> baseUrls = new ArrayList<CloudBaseUrl>();

        if (tenant.getBaseUrlIds() != null) {
            Boolean v1DefaultExist = false;
            if (tenant.getV1Defaults() != null) {
                v1DefaultExist = true;
            }
            Boolean baseUrlContainsV1Default = false;
            for (String baseUrlId : tenant.getBaseUrlIds()) {
                CloudBaseUrl baseUrl = this.getBaseUrlById(Integer.parseInt(baseUrlId));
                if (v1DefaultExist) {
                    baseUrlContainsV1Default = Arrays.asList(tenant.getV1Defaults()).contains(String.valueOf(baseUrlId));
                }
                if (baseUrl != null && !baseUrlContainsV1Default) {
                    //Add Tenant to end of baseurl
                    baseUrl.setV1Default(false);
                    appendTenantToBaseUrl(tenant.getName(), baseUrl);
                    baseUrls.add(baseUrl);
                }
            }
            if (v1DefaultExist) {
                for (String v1DefaultId : tenant.getV1Defaults()) {
                    CloudBaseUrl v1Default = this.getBaseUrlById(Integer.parseInt(v1DefaultId));
                    if (v1Default != null) {
                        v1Default.setV1Default(true);
                        appendTenantToBaseUrl(tenant.getName(), v1Default);
                        baseUrls.add(v1Default);
                    }
                }
            }
        }

        OpenstackEndpoint point = new OpenstackEndpoint();
        point.setTenantId(tenant.getTenantId());
        point.setTenantName(tenant.getName());
        point.setBaseUrls(baseUrls);

        return point;
    }

    void appendTenantToBaseUrl(String tenantId, CloudBaseUrl baseUrl) {
        String publicUrl = baseUrl.getPublicUrl();
        if(publicUrl != null) {
            publicUrl = publicUrl.substring(0, publicUrl.length() - (publicUrl.endsWith("/") ? 1 : 0)) + "/" + tenantId;
            baseUrl.setPublicUrl(publicUrl);
        }
        String adminUrl = baseUrl.getAdminUrl();
        if(adminUrl != null) {
            adminUrl = adminUrl.substring(0, adminUrl.length() - (adminUrl.endsWith("/") ? 1 : 0)) + "/" + tenantId;
            baseUrl.setAdminUrl(adminUrl);
        }
        String internalUrl = baseUrl.getInternalUrl();
        if(internalUrl != null) {
            internalUrl = internalUrl.substring(0, internalUrl.length() - (internalUrl.endsWith("/") ? 1 : 0)) + "/" + tenantId;
            baseUrl.setInternalUrl(internalUrl);
        }
    }

    @Override
    public void removeBaseUrlFromUser(int baseUrlId, String username) {
        getLogger().debug("Removing baseurl {} from user {}", baseUrlId, username);

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
            // the attribute from LDAP
            mods.add(new Modification(ModificationType.DELETE, ATTR_ENDPOINT));
        } else {
            // Else we'll just replace all the values for endpoints with the
            // reduced list.
            String[] points = newEndpoints.toArray(new String[newEndpoints.size()]);
            mods.add(new Modification(ModificationType.REPLACE, ATTR_ENDPOINT, points));
        }
        Audit audit = Audit.log("dn="+endpoints.getUserDN()).modify(mods);

        this.updateEntry(endpoints.getUserDN(), mods, audit);

        audit.succeed();

        getLogger().debug("Removed baseurl {} from user {}", baseUrlId, username);
    }

    @Override
    public void setBaseUrlEnabled(int baseUrlId, boolean enabled) {
        getLogger().debug("Setting baseurl {} enabled {}", baseUrlId, enabled);

        CloudBaseUrl baseUrl = this.getBaseUrlById(baseUrlId);
        if (baseUrl == null) {
            String errMsg = String.format("BaseUrlId %s not found", baseUrlId);
            getLogger().error(errMsg);
            throw new NotFoundException(errMsg);
        }

        List<Modification> mods = new ArrayList<Modification>();
        mods.add(new Modification(ModificationType.REPLACE, ATTR_ENABLED,
                String.valueOf(enabled)));

        Audit audit = Audit.log(baseUrl).modify(mods);

        this.updateEntry(baseUrl.getUniqueId(), mods, audit);

        audit.succeed();

        getLogger().debug("Set baseurl {} enabled {}", baseUrlId, enabled);
    }

    @Override
    public void updateCloudBaseUrl(CloudBaseUrl cloudBaseUrl) {
        if (cloudBaseUrl == null || StringUtils.isBlank(cloudBaseUrl.getUniqueId())) {
            String errmsg = "Null instance of BaseUrl was passed";
            getLogger().error(errmsg);
            throw new IllegalArgumentException(errmsg);
        }
        getLogger().debug("Updating BaseURL: {}", cloudBaseUrl);
        Audit audit = Audit.log(cloudBaseUrl);
        CloudBaseUrl oldBaseUrl = getBaseUrlById(cloudBaseUrl.getBaseUrlId());
        List<Modification> modifications = getModifications(oldBaseUrl, cloudBaseUrl);
        audit.modify(modifications);
        if (modifications.size() > 0) {
            updateEntry(oldBaseUrl.getUniqueId(), modifications, audit);
        }
        getLogger().debug("Updated CloudBaseUrl: {}", cloudBaseUrl);
        audit.succeed();
    }

    CloudBaseUrl getBaseUrl(SearchResultEntry resultEntry) {
        if(resultEntry == null){
            return null;
        }
        getLogger().debug("Inside getBaseUrl");
        CloudBaseUrl baseUrl = new CloudBaseUrl();
        baseUrl.setUniqueId(resultEntry.getDN());
        baseUrl.setAdminUrl(resultEntry.getAttributeValue(ATTR_ADMIN_URL));
        baseUrl.setBaseUrlId(resultEntry.getAttributeValueAsInteger(ATTR_ID));
        baseUrl.setBaseUrlType(resultEntry.getAttributeValue(ATTR_BASEURL_TYPE));
        baseUrl.setDef(resultEntry.getAttributeValueAsBoolean(ATTR_DEF));
        baseUrl.setInternalUrl(resultEntry.getAttributeValue(ATTR_INTERNAL_URL));
        baseUrl.setPublicUrl(resultEntry.getAttributeValue(ATTR_PUBLIC_URL));
        baseUrl.setRegion(resultEntry.getAttributeValue(ATTR_REGION));
        baseUrl.setServiceName(resultEntry.getAttributeValue(ATTR_SERVICE));
        baseUrl.setEnabled(resultEntry.getAttributeValueAsBoolean(ATTR_ENABLED));
        //baseUrl.setName(resultEntry.getAttributeValue(ATTR_NAME));
        baseUrl.setOpenstackType(resultEntry.getAttributeValue(ATTR_OPENSTACK_TYPE));
        baseUrl.setVersionId(resultEntry.getAttributeValue(ATTR_VERSION_ID));
        baseUrl.setVersionInfo(resultEntry.getAttributeValue(ATTR_VERSION_INFO));
        baseUrl.setVersionList(resultEntry.getAttributeValue(ATTR_VERSION_LIST));
        baseUrl.setGlobal(resultEntry.getAttributeValueAsBoolean(ATTR_GLOBAL));

        getLogger().debug("Exiting getBaseUrl");
        return baseUrl;
    }

    EndPoints getRawEndpointsForUser(String username) {
        getLogger().debug("Inside getRawEndpointsForUser {}", username);
        List<String> userEndpoints = new ArrayList<String>();
        String userDN = null;
        String nastId = null;
        Integer mossoId = null;
        SearchResultEntry searchResult = null;

        Filter searchFilter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_UID, username)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON)
                .build();

        searchResult = getSingleEntry(BASE_DN, SearchScope.SUB, searchFilter, new String[]{ATTR_ENDPOINT, ATTR_UID, ATTR_NAST_ID, ATTR_MOSSO_ID});

        if (searchResult == null) {
            String errMsg = String.format("User %s not found.", username);
            getLogger().error(errMsg);
            throw new NotFoundException(errMsg);
        }

        String[] list = searchResult.getAttributeValues(ATTR_ENDPOINT);
        if (list != null && list.length > 0) {
            userEndpoints = Arrays.asList(list);
        }
        userDN = searchResult.getDN();
        nastId = searchResult.getAttributeValue(ATTR_NAST_ID);
        mossoId = searchResult.getAttributeValueAsInteger(ATTR_MOSSO_ID);

        EndPoints points = new EndPoints();
        points.setMossoId(mossoId);
        points.setUsername(username);
        points.setNastId(nastId);
        points.setEndpoints(userEndpoints);
        points.setUserDN(userDN);

        getLogger().debug("Exiting getRawEndpointsForUser {}", username);
        return points;
    }

    List<Modification> getModifications(CloudBaseUrl gOld, CloudBaseUrl gNew) {
        List<Modification> mods = new ArrayList<Modification>();

        if (gNew.getDef() != null && !gNew.getDef().equals(gOld.getDef())) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_DEF, String.valueOf(gNew.getDef())));
        }

        if (gNew.getAdminUrl() != null && !gNew.getAdminUrl().equals(gOld.getAdminUrl())) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_ADMIN_URL, String.valueOf(gNew.getAdminUrl())));
        }

        if (gNew.getBaseUrlType() != null && !gNew.getBaseUrlType().equals(gOld.getBaseUrlType())) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_BASEURL_TYPE, String.valueOf(gNew.getBaseUrlType())));
        }

        if (gNew.getEnabled() != null && !gNew.getEnabled().equals(gOld.getEnabled())) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_ENABLED, String.valueOf(gNew.getEnabled())));
        }

        if (gNew.getGlobal() != null && !gNew.getGlobal().equals(gOld.getGlobal())) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_GLOBAL, String.valueOf(gNew.getGlobal())));
        }

        if (gNew.getInternalUrl() != null && !gNew.getInternalUrl().equals(gOld.getInternalUrl())) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_INTERNAL_URL, String.valueOf(gNew.getInternalUrl())));
        }

        if (gNew.getPublicUrl() != null && !gNew.getPublicUrl().equals(gOld.getPublicUrl())) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_PUBLIC_URL, String.valueOf(gNew.getPublicUrl())));
        }

        if (gNew.getRegion() != null && !gNew.getRegion().equals(gOld.getRegion())) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_REGION, String.valueOf(gNew.getRegion())));
        }

        /*
        if (gNew.getVersionId() != null && !gNew.getVersionId().equals(gOld.getVersionId())) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_VERSION_ID, String.valueOf(gNew.getVersionId())));
        }

        if (gNew.getVersionInfo() != null && !gNew.getVersionInfo().equals(gOld.getVersionInfo())) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_VERSION_INFO, String.valueOf(gNew.getVersionInfo())));
        }

        if (gNew.getVersionList() != null && !gNew.getVersionList().equals(gOld.getVersionList())) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_VERSION_LIST, String.valueOf(gNew.getVersionList())));
        }
        */
        
        if (gNew.getServiceName() != null && !gNew.getServiceName().equals(gOld.getServiceName())) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_SERVICE, String.valueOf(gNew.getServiceName())));
        }

        if (gNew.getOpenstackType() != null && !gNew.getOpenstackType().equals(gOld.getOpenstackType())) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_OPENSTACK_TYPE, String.valueOf(gNew.getOpenstackType())));
        }

        return mods;
    }
}
