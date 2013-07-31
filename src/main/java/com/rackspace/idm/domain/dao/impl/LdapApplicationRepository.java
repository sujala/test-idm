package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.service.PropertiesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.dao.ApplicationDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.entity.FilterParam.FilterParamName;
import com.rackspace.idm.util.CryptHelper;
import com.unboundid.ldap.sdk.*;
import org.apache.commons.lang.StringUtils;
import org.bouncycastle.crypto.InvalidCipherTextException;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

@Component
public class LdapApplicationRepository extends LdapGenericRepository<Application> implements ApplicationDao {

    public static final String ENCRYPTION_ERROR = "encryption error";
    public static final String FOUND_CLIENT = "Found client - {}";

    @Autowired
    CryptHelper cryptHelper;

    @Autowired
    PropertiesService propertiesService;

    public String getBaseDn(){
        return APPLICATIONS_BASE_DN;
    }

    public String getSoftDeletedBaseDn() {
        return SOFT_DELETED_APPLICATIONS_BASE_DN;
    }

    public String getLdapEntityClass(){
        return OBJECTCLASS_RACKSPACEAPPLICATION;
    }

    @Override
    public void addClient(Application application) {
        if (application == null) {
            String errMsg = "Null instance of Client was passed in.";
            getLogger().error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }
        encryptPassword(application);
        addObject(application);
        application.setClientSecretObj(application.getClientSecretObj().toExisting());
    }

    private void encryptPassword(Application application) {
        if (application == null) {
            return;
        }

        String versionId = getEncryptionVersionId(application);
        String salt = getSalt(application);

        if (application.getClientSecretObj() != null && application.getClientSecretObj().isNew()) {
            try {
                application.setClearPasswordBytes(cryptHelper.encrypt(application.getClientSecret(), versionId, salt));
            } catch (GeneralSecurityException e) {
                getLogger().error(e.getMessage());
                throw new IllegalStateException(e);
            } catch (InvalidCipherTextException e) {
                getLogger().error(e.getMessage());
                throw new IllegalStateException(e);
            }
        }
    }

    private void decryptPassword(Application application) {
        if (application == null) {
            return;
        }

        String versionId = getEncryptionVersionId(application);
        String salt = getSalt(application);

        try {
            String password = cryptHelper.decrypt(application.getClearPasswordBytes(), versionId, salt);
            ClientSecret secret = ClientSecret.existingInstance(password);
            application.setClientSecretObj(secret);
            application.setClearPassword(password);
        } catch (GeneralSecurityException e) {
            getLogger().error(e.getMessage());
            throw new IllegalStateException(e);
        } catch (InvalidCipherTextException e) {
            getLogger().error(e.getMessage());
            throw new IllegalStateException(e);
        }
    }

    private void decryptPasswords(List<Application> applications) {
        if (applications != null && applications.size() > 0) {
            for (Application application : applications) {
                decryptPassword(application);
            }
        }
    }

    @Override
    public ClientAuthenticationResult authenticate(String clientId, String clientSecret) {
        BindResult result;
        Application client = getApplicationByClientId(clientId);

        if (client == null) {
            getLogger().debug("Client {} could not be found.", clientId);
            return new ClientAuthenticationResult(null, false);
        }

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
        if (application != null) {
            deleteObject(application);
        }
    }

    @Override
    public List<Application> getAllApplications() {
        return DecryptApplicationsPasswords(searchFilterGetApplications());
    }

    @Override
    public Application getApplicationByClientId(String clientId) {
        if (StringUtils.isBlank(clientId)) {
            return null;
        }
        return DecryptApplicationPassword(searchFilterGetApplicationByClientId(clientId));
    }

    @Override
    public Application getApplicationByName(String name) {
        if (StringUtils.isBlank(name)) {
            getLogger().error("Null or Empty application name parameter");
            throw new IllegalArgumentException("Null or Empty client name parameter.");
        }
        return DecryptApplicationPassword(searchFilterGetApplicationByName(name));
    }

    @Override
    public Application getApplicationByCustomerIdAndClientId(String customerId, String clientId) {
        if (StringUtils.isBlank(customerId)) {
            getLogger().error("Null or Empty customer Id parameter");
            throw new IllegalArgumentException("Null or Empty client name parameter.");
        }
        if (StringUtils.isBlank(clientId)) {
            return null;
        }
        return DecryptApplicationPassword(searchFilterGetApplicationByCustomerIdAndClientId(customerId, clientId));
    }

    private Application DecryptApplicationPassword(Filter filter) {
        Application app = getObject(filter);
        decryptPassword(app);
        return app;
    }

    private List<Application> DecryptApplicationsPasswords(Filter filter) {
        List<Application> apps = getObjects(filter);
        decryptPasswords(apps);
        return apps;
    }

    @Override
    public Application getApplicationByScope(String scope) {
        if (StringUtils.isBlank(scope)) {
            getLogger().error("Null or Empty application scope parameter");
            throw new IllegalArgumentException("Null or Empty client name parameter.");
        }
        return DecryptApplicationPassword(searchFilterGetApplicationByScope(scope));
    }

    @Override
    public Applications getClientsByCustomerId(String customerId, int offset,
                                               int limit) {
        getLogger().debug("Doing search for customerId {}", customerId);

        if (StringUtils.isBlank(customerId)) {
            getLogger().error("Null or Empty customerId parameter");
            throw new IllegalArgumentException(
                    "Null or Empty customerId parameter.");
        }

        Filter searchFilter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_RACKSPACE_CUSTOMER_NUMBER, customerId)
                .addEqualAttribute(ATTR_OBJECT_CLASS,
                        OBJECTCLASS_RACKSPACEAPPLICATION).build();

        Applications clients = getMultipleClients(searchFilter, offset, limit);

        getLogger().debug("Found {} clients for customer {}",
                clients.getTotalRecords(), customerId);

        return clients;
    }

    @Override
    public Applications getAllApplications(List<FilterParam> filters, int offset, int limit) {
        getLogger().debug("Getting all applications");

        LdapSearchBuilder searchBuilder = new LdapSearchBuilder();
        searchBuilder.addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEAPPLICATION);

        if (filters != null) {
            for (FilterParam filter : filters) {
                // can only filter on application name for now
                if (filter.getParam() == FilterParamName.APPLICATION_NAME) {
                    searchBuilder.addEqualAttribute(ATTR_NAME, filter.getStrValue());
                }
            }
        }

        Filter searchFilter = searchBuilder.build();

        Applications applications = getMultipleClients(searchFilter, offset, limit);

        getLogger().debug("Got {} applications", applications.getTotalRecords());

        return applications;
    }

    @Override
    public void updateApplication(Application application) {
        encryptPassword(application);
        updateObject(application);
        application.setClientSecretObj(application.getClientSecretObj().toExisting());
    }

    @Override
    public List<Application> getAvailableScopes() {
        return DecryptApplicationsPasswords(searchFilterGetAvailableScopes());
    }

    private String getEncryptionVersionId(Application application) {
        if (application.getEncryptionVersion() == null) {
            return "0";
        } else {
            return application.getEncryptionVersion();
        }
    }

    private String getSalt(Application application) {
        if (application.getSalt() == null) {
            return config.getString("crypto.salt");
        } else {
            return application.getSalt();
        }
    }

    Application getSingleSoftDeletedClient(Filter searchFilter) {
        return getObject(searchFilter, SOFT_DELETED_APPLICATIONS_BASE_DN);
    }

    @Override
    public List<Application> getOpenStackServices() {
        return getObjects(searchFilterGetOpenstackServices());
    }

    @Override
    public void softDeleteApplication(Application application) {
        softDeleteObject(application);
    }

    @Override
    public Application getSoftDeletedApplicationById(String id) {
        if (StringUtils.isBlank(id)) {
            getLogger().error("Null or Empty id parameter");
            throw new IllegalArgumentException(
                    "Null or Empty id parameter.");
        }
        return getObject(searchFilterGetApplicationByClientId(id), getSoftDeletedBaseDn());
    }

    @Override
    public Application getSoftDeletedClientByName(String clientName) {
        if (StringUtils.isBlank(clientName)) {
            getLogger().error("Null or Empty clientName parameter");
            throw new IllegalArgumentException(
                    "Null or Empty clientName parameter.");
        }
        return getObject(searchFilterGetApplicationByName(clientName), getSoftDeletedBaseDn());
    }

    @Override
    public void unSoftDeleteApplication(Application application) {
        unSoftDeleteObject(application);
    }

    public void setCryptHelper(CryptHelper cryptHelper) {
        this.cryptHelper = cryptHelper;
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

    private Filter searchFilterGetApplicationById(String id) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_ID, id)
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

    Application getClientOld(SearchResultEntry resultEntry) {
        Application client = new Application();
        client.setClientId(resultEntry.getAttributeValue(ATTR_CLIENT_ID));

        client.setEncryptionVersion(resultEntry.getAttributeValue(ATTR_ENCRYPTION_VERSION_ID));
        client.setSalt(resultEntry.getAttributeValue(ATTR_ENCRYPTION_SALT));

        String versionId = getEncryptionVersionId(client);
        String salt = getSalt(client);

        try {
            String ecryptedPwd = cryptHelper.decrypt(resultEntry.getAttributeValueBytes(ATTR_CLEAR_PASSWORD), versionId, salt);
            ClientSecret secret = ClientSecret.existingInstance(ecryptedPwd);
            client.setClientSecretObj(secret);
        } catch (GeneralSecurityException e) {
            getLogger().error(e.getMessage());
            throw new IllegalStateException(e);
        } catch (InvalidCipherTextException e) {
            getLogger().error(e.getMessage());
            throw new IllegalStateException(e);
        }

        client.setName(resultEntry.getAttributeValue(ATTR_NAME));

        client.setRcn(resultEntry.getAttributeValue(ATTR_RACKSPACE_CUSTOMER_NUMBER));

        client.setOpenStackType(resultEntry.getAttributeValue(ATTR_OPENSTACK_TYPE));

        client.setEnabled(resultEntry.getAttributeValueAsBoolean(ATTR_ENABLED));

        client.setCallBackUrl(resultEntry.getAttributeValue(ATTR_CALLBACK_URL));
        client.setTitle(resultEntry.getAttributeValue(ATTR_TITLE));
        client.setDescription(resultEntry.getAttributeValue(ATTR_DESCRIPTION));
        client.setScope(resultEntry.getAttributeValue(ATTR_TOKEN_SCOPE));
        client.setUseForDefaultRegion(resultEntry.getAttributeValueAsBoolean(ATTR_USE_FOR_DEFAULT_REGION));

        getLogger().debug("Materialized Client object {}.", client);
        return client;
    }

    Applications getMultipleClients(Filter searchFilter, int offset, int limit) {

        int offsets = offset < 0 ? this.getLdapPagingOffsetDefault() : offset;
        int limits = limit <= 0 ? this.getLdapPagingLimitDefault() : limit;
        limits = limits > this.getLdapPagingLimitMax() ? this.getLdapPagingLimitMax() : limits;

        int contentCount = 0;

        List<Application> clientList = new ArrayList<Application>();

        List<SearchResultEntry> entries = this.getMultipleEntries(APPLICATIONS_BASE_DN, SearchScope.SUB, searchFilter, null);

        contentCount = entries.size();

        if (offsets < contentCount) {

            int toIndex = offsets + limits > contentCount ? contentCount : offsets + limits;
            int fromIndex = offsets;

            List<SearchResultEntry> subList = entries.subList(fromIndex, toIndex);

            for (SearchResultEntry entry : subList) {
                clientList.add(getClientOld(entry));
            }
        }

        getLogger().debug("Found {} clients.", clientList.size());

        Applications clients = new Applications();

        clients.setLimit(limits);
        clients.setOffset(offsets);
        clients.setTotalRecords(contentCount);
        clients.setClients(clientList);

        return clients;
    }

}
