package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.entity.PaginatorContext;
import com.rackspace.idm.domain.service.EncryptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.dao.ApplicationDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.util.CryptHelper;
import com.unboundid.ldap.sdk.*;

import java.util.List;

@Component
public class LdapApplicationRepository extends LdapGenericRepository<Application> implements ApplicationDao {

    @Autowired
    CryptHelper cryptHelper;

    @Autowired
    EncryptionService encryptionService;

    public String getBaseDn(){
        return APPLICATIONS_BASE_DN;
    }

    public String getSoftDeletedBaseDn() {
        return SOFT_DELETED_APPLICATIONS_BASE_DN;
    }

    public String getLdapEntityClass(){
        return OBJECTCLASS_RACKSPACEAPPLICATION;
    }

    public String getSortAttribute() {
        return ATTR_CLIENT_ID;
    }

    @Override
    public void doPreEncode(Application application) {
        encryptionService.encryptApplication(application);
    }

    @Override
    public void doPostEncode(Application application) {
        encryptionService.decryptApplication(application);
    }

    @Override
    public void addApplication(Application application) {
        addObject(application);
        application.setClientSecretObj(application.getClientSecretObj().toExisting());
    }

    @Override
    public ClientAuthenticationResult authenticate(String clientId, String clientSecret) {
        BindResult result;
        Application client = getApplicationByClientId(clientId);

        Audit audit = Audit.authClient(client);

        try {
            result = getBindConnPool().bind(client.getUniqueId(), clientSecret);
            audit.succeed();
        } catch (LDAPException e) {
            audit.fail(e.getMessage());
            if (ResultCode.INVALID_CREDENTIALS.equals(e.getResultCode())) {
                return new ClientAuthenticationResult(client, false);
            }
            getLogger().error("Bind operation on clientId " + clientId + " failed.", e);
            throw new IllegalStateException(e.getMessage(), e);
        }

        boolean isAuthenticated = ResultCode.SUCCESS.equals(result.getResultCode());
        getLogger().debug("Client {} authenticated == {}", isAuthenticated);
        return new ClientAuthenticationResult(client, isAuthenticated);
    }

    @Override
    public void deleteApplication(Application application) {
            deleteObject(application);
    }

    @Override
    public Iterable<Application> getAllApplications() {
        return getObjects(searchFilterGetApplications());
    }

    @Override
    public Application getApplicationByClientId(String clientId) {
        return getObject(searchFilterGetApplicationByClientId(clientId));
    }

    @Override
    public Application getApplicationByName(String name) {
        return getObject(searchFilterGetApplicationByName(name));
    }

    @Override
    public Application getApplicationByCustomerIdAndClientId(String customerId, String clientId) {
        return getObject(searchFilterGetApplicationByCustomerIdAndClientId(customerId, clientId));
    }

    @Override
    public Application getApplicationByScope(String scope) {
        return getObject(searchFilterGetApplicationByScope(scope));
    }

    @Override
    public Applications getApplicationsByCustomerId(String customerId, int offset, int limit) {
        PaginatorContext<Application> page = getObjectsPaged(searchFilterGetApplicationsByCustomerId(customerId), offset, limit);
        Applications apps = new Applications();
        apps.setClients(page.getValueList());
        apps.setLimit(page.getLimit());
        apps.setOffset(page.getOffset());
        apps.setTotalRecords(page.getTotalRecords());
        return apps;
    }

    @Override
    public PaginatorContext<Application> getApplicationsByCustomerIdPaged(String customerId, int offset, int limit) {
        return getObjectsPaged(searchFilterGetApplicationsByCustomerId(customerId), offset, limit);
    }

    @Override
    public Applications getAllApplications(List<FilterParam> filters, int offset, int limit) {
        PaginatorContext<Application> page = getObjectsPaged(searchFilterGetApplications(), offset, limit);
        Applications apps = new Applications();
        apps.setClients(page.getValueList());
        apps.setLimit(page.getLimit());
        apps.setOffset(page.getOffset());
        apps.setTotalRecords(page.getTotalRecords());
        return apps;
    }

    @Override
    public PaginatorContext<Application> getAllApplicationsPaged(List<FilterParam> filters, int offset, int limit) {
        return getObjectsPaged(searchFilterGetApplications(), offset, limit);
    }

    @Override
    public void updateApplication(Application application) {
        updateObject(application);
        application.setClientSecretObj(application.getClientSecretObj().toExisting());
    }

    @Override
    public Iterable<Application> getAvailableScopes() {
        return getObjects(searchFilterGetAvailableScopes());
    }

    @Override
    public Iterable<Application> getOpenStackServices() {
        return getObjects(searchFilterGetOpenstackServices());
    }

    @Override
    public void softDeleteApplication(Application application) {
        softDeleteObject(application);
    }

    @Override
    public Application getSoftDeletedApplicationById(String id) {
        return getObject(searchFilterGetApplicationByClientId(id), getSoftDeletedBaseDn());
    }

    @Override
    public Application getSoftDeletedClientByName(String clientName) {
        return getObject(searchFilterGetApplicationByName(clientName), getSoftDeletedBaseDn());
    }

    @Override
    public void unSoftDeleteApplication(Application application) {
        unSoftDeleteObject(application);
    }

    private Filter searchFilterGetApplications() {
        return new LdapRepository.LdapSearchBuilder()
                .addEqualAttribute(LdapRegionRepository.ATTR_OBJECT_CLASS, LdapRegionRepository.OBJECTCLASS_RACKSPACEAPPLICATION).build();
    }

    private Filter searchFilterGetApplicationByClientId(String clientId) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_CLIENT_ID, clientId)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEAPPLICATION)
                .build();
    }

    private Filter searchFilterGetApplicationByName(String name) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_NAME, name)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEAPPLICATION)
                .build();
    }

    private Filter searchFilterGetApplicationByCustomerIdAndClientId(String customerId, String clientId) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_CLIENT_ID, clientId)
                .addEqualAttribute(ATTR_RACKSPACE_CUSTOMER_NUMBER, customerId)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEAPPLICATION)
                .build();
    }

    private Filter searchFilterGetApplicationByScope(String scope) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_TOKEN_SCOPE, scope)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEAPPLICATION)
                .build();
    }

    private Filter searchFilterGetApplicationsByCustomerId(String customerId) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_RACKSPACE_CUSTOMER_NUMBER, customerId)
                .addEqualAttribute(ATTR_OBJECT_CLASS,
                                   OBJECTCLASS_RACKSPACEAPPLICATION).build();
    }

    private Filter searchFilterGetAvailableScopes() {
        return new LdapSearchBuilder()
                .addPresenceAttribute(ATTR_TOKEN_SCOPE)
                .addEqualAttribute(ATTR_OBJECT_CLASS,
                                   OBJECTCLASS_RACKSPACEAPPLICATION).build();
    }

    private Filter searchFilterGetOpenstackServices() {
        return new LdapSearchBuilder()
                .addPresenceAttribute(ATTR_OPENSTACK_TYPE)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEAPPLICATION)
                .build();
    }

    protected int getLdapPagingOffsetDefault() {
        return config.getInt("ldap.paging.offset.default");
    }

    protected int getLdapPagingLimitDefault() {
        return config.getInt("ldap.paging.limit.default");
    }
}
