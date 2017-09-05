package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.LDAPComponent;
import com.rackspace.idm.domain.dao.ApplicationDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.EncryptionService;
import com.rackspace.idm.util.CryptHelper;
import com.unboundid.ldap.sdk.Filter;
import org.springframework.beans.factory.annotation.Autowired;


@LDAPComponent
public class LdapApplicationRepository extends LdapGenericRepository<Application> implements ApplicationDao {

    @Autowired
    CryptHelper cryptHelper;

    @Autowired
    EncryptionService encryptionService;

    public String getBaseDn(){
        return APPLICATIONS_BASE_DN;
    }

    public String getLdapEntityClass(){
        return OBJECTCLASS_RACKSPACEAPPLICATION;
    }

    public String getSortAttribute() {
        return ATTR_CLIENT_ID;
    }

    @Override
    public void addApplication(Application application) {
        addObject(application);
        application.setClientSecretObj(application.getClientSecretObj().toExisting());
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
    public Iterable<Application> getApplicationByType(String type) {
        return getObjects(searchFilterGetApplicationByType(type));
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
    public void updateApplication(Application application) {
        updateObject(application);
        application.setClientSecretObj(application.getClientSecretObj().toExisting());
    }

    @Override
    public Iterable<Application> getOpenStackServices() {
        return getObjects(searchFilterGetOpenstackServices());
    }

    @Override
    public PaginatorContext<Application> getOpenStackServices(int offset, int limit) {
        return getObjectsPaged(searchFilterGetOpenstackServices(), offset, limit);
    }

    private Filter searchFilterGetApplications() {
        return new LdapRepository.LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEAPPLICATION).build();
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

    private Filter searchFilterGetApplicationByType(String type) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OPENSTACK_TYPE, type)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEAPPLICATION)
                .build();
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
