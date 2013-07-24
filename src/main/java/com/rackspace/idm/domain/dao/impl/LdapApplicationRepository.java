package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.service.PropertiesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.dao.ApplicationDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.entity.FilterParam.FilterParamName;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.util.CryptHelper;
import com.unboundid.ldap.sdk.*;
import com.unboundid.ldap.sdk.persist.LDAPPersistException;
import com.unboundid.ldap.sdk.persist.LDAPPersister;
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
        encryptPassword(application);
        addObject(application);
    }

    private void encryptPassword(Application application) {
        String versionId = getEncryptionVersionId(application);
        String salt = getSalt(application);

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

    private void decryptPassword(Application application) {
        if (application != null) {

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
    public void deleteClient(Application application) {
        deleteObject(application);
    }

    @Override
    public List<Application> getAllApplications() {
        return DecryptApplicationsPasswords(searchFilterGetApplications());
    }

    @Override
    public Application getApplicationByClientId(String clientId) {
        return DecryptApplicationPassword(searchFilterGetApplicationByClientId(clientId));
    }

    @Override
    public Application getApplicationByName(String name) {
        return DecryptApplicationPassword(searchFilterGetApplicationByName(name));
    }

    @Override
    public Application getApplicationByCustomerIdAndClientId(String customerId, String clientId) {
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
    public Application getApplicationById(String id) {
        return DecryptApplicationPassword(searchFilterGetApplicationById(id));
    }

    @Override
    public Application getApplicationByScope(String scope) {
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
    public Applications getAllClients(List<FilterParam> filters, int offset, int limit) {
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
        getLogger().debug("Updating application {}", application);

        if (application == null || StringUtils.isBlank(application.getClientId())) {
            getLogger().error("Application instance is null or its clientId has no value");
            throw new IllegalArgumentException("Bad parameter: The Application instance either null or its clientName has no value.");
        }
        String clientId = application.getClientId();
        Application oldClient = getApplicationByClientId(clientId);

        if (oldClient == null) {
            getLogger().error("No record found for application {}", clientId);
            throw new IllegalArgumentException("There is no existing record for the given application instance.");
        }

        Audit audit = Audit.log(application);
        List<Modification> mods;
        try {
            mods = getModifications(oldClient, application);
            if (mods.size() < 1) {
                // No changes!
                return;
            }
            audit.modify(mods);

            updateEntry(oldClient.getUniqueId(), mods, audit);
        } catch (GeneralSecurityException e) {
            getLogger().error(e.getMessage());
            audit.fail(ENCRYPTION_ERROR);
            throw new IllegalStateException(e);
        } catch (InvalidCipherTextException e) {
            getLogger().error(e.getMessage());
            audit.fail(ENCRYPTION_ERROR);
            throw new IllegalStateException(e);
        }

        audit.succeed();
        getLogger().debug("Updated application {}", application.getName());
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

    Attribute[] getAddAttributesForClient(Application client) throws InvalidCipherTextException, GeneralSecurityException {
        List<Attribute> atts = new ArrayList<Attribute>();

        String versionId = getEncryptionVersionId(client);
        String salt = getSalt(client);

        if (!StringUtils.isBlank(client.getEncryptionVersion())) {
            atts.add(new Attribute(ATTR_ENCRYPTION_VERSION_ID, client.getEncryptionVersion()));
        }

        if (!StringUtils.isBlank(client.getSalt())) {
            atts.add(new Attribute(ATTR_ENCRYPTION_SALT, client.getSalt()));
        }

        atts.add(new Attribute(ATTR_OBJECT_CLASS, ATTR_CLIENT_OBJECT_CLASS_VALUES));

        if (!StringUtils.isBlank(client.getClientId())) {
            atts.add(new Attribute(ATTR_CLIENT_ID, client.getClientId()));
        }

        if (!StringUtils.isBlank(client.getOpenStackType())) {
            atts.add(new Attribute(ATTR_OPENSTACK_TYPE, client.getOpenStackType()));
        }

        if (!StringUtils.isBlank(client.getName())) {
            atts.add(new Attribute(ATTR_NAME, client.getName()));
        }

        if (!StringUtils.isBlank(client.getRcn())) {
            atts.add(new Attribute(ATTR_RACKSPACE_CUSTOMER_NUMBER, client.getRcn()));
        }

        if (!StringUtils.isBlank(client.getClientSecretObj().getValue())) {
            atts.add(new Attribute(ATTR_CLIENT_SECRET, client.getClientSecret()));
            atts.add(new Attribute(ATTR_CLEAR_PASSWORD, cryptHelper.encrypt(client.getClientSecret(), versionId, salt)));
        }

        if (client.getEnabled() != null) {
            atts.add(new Attribute(ATTR_ENABLED, String.valueOf(client.getEnabled()).toUpperCase()));
        }

        if (!StringUtils.isBlank(client.getTitle())) {
            atts.add(new Attribute(ATTR_TITLE, client.getTitle()));
        }

        if (!StringUtils.isBlank(client.getDescription())) {
            atts.add(new Attribute(ATTR_DESCRIPTION, client.getDescription()));
        }

        if (!StringUtils.isBlank(client.getScope())) {
            atts.add(new Attribute(ATTR_TOKEN_SCOPE, client.getScope()));
        }

        if (!StringUtils.isBlank(client.getCallBackUrl())) {
            atts.add(new Attribute(ATTR_CALLBACK_URL, client.getCallBackUrl()));
        }

        if (client.getUseForDefaultRegion() != null) {
            atts.add(new Attribute(ATTR_USE_FOR_DEFAULT_REGION, String.valueOf(client.getUseForDefaultRegion()).toUpperCase()));
        }

        Attribute[] attributes = atts.toArray(new Attribute[0]);
        getLogger().debug("Found {} attributes for client {}.", attributes.length, client);
        return attributes;
    }

    Application getSingleSoftDeletedClient(Filter searchFilter) {
        return getObject(searchFilter, SOFT_DELETED_APPLICATIONS_BASE_DN);
    }

    List<Modification> getModifications(Application cOld, Application cNew) throws InvalidCipherTextException, GeneralSecurityException {
        List<Modification> mods = new ArrayList<Modification>();

        checkForRCNModification(cOld, cNew, mods);
        checkForClientSecretModification(cNew, cryptHelper, mods);
        checkForEnabledStatusModification(cOld, cNew, mods);
        checkForTitleModification(cOld, cNew, mods);
        checkForDescriptionModification(cOld, cNew, mods);
        checkForScopeModification(cOld, cNew, mods);
        checkForCallBackUrlModification(cOld, cNew, mods);
        checkForUseForDefaultRegionModification(cOld,cNew,mods);

        getLogger().debug("Found {} modifications.", mods.size());

        return mods;
    }

    private void checkForCallBackUrlModification(Application cOld, Application cNew, List<Modification> mods) {
        if (cNew.getCallBackUrl() != null) {
            if (StringUtils.isBlank(cNew.getCallBackUrl())) {
                mods.add(new Modification(ModificationType.DELETE, ATTR_CALLBACK_URL));
            } else if (!StringUtils.equals(cOld.getCallBackUrl(), cNew.getCallBackUrl())) {
                mods.add(new Modification(ModificationType.REPLACE, ATTR_CALLBACK_URL, cNew.getCallBackUrl()));
            }
        }
    }

    private void checkForScopeModification(Application cOld, Application cNew, List<Modification> mods) {
        if (cNew.getScope() != null) {
            if (StringUtils.isBlank(cNew.getScope())) {
                mods.add(new Modification(ModificationType.DELETE, ATTR_TOKEN_SCOPE));
            } else if (!StringUtils.equals(cOld.getScope(), cNew.getScope())) {
                mods.add(new Modification(ModificationType.REPLACE,
                        ATTR_TOKEN_SCOPE, cNew.getScope()));
            }
        }
    }

    private void checkForDescriptionModification(Application cOld, Application cNew, List<Modification> mods) {
        if (cNew.getDescription() != null) {
            if (StringUtils.isBlank(cNew.getDescription())) {
                mods.add(new Modification(ModificationType.DELETE, ATTR_DESCRIPTION));
            } else if (!StringUtils.equals(cOld.getDescription(), cNew.getDescription())) {
                mods.add(new Modification(ModificationType.REPLACE, ATTR_DESCRIPTION, cNew.getDescription()));
            }
        }
    }

    private void checkForTitleModification(Application cOld, Application cNew, List<Modification> mods) {
        if (cNew.getTitle() != null) {
            if (StringUtils.isBlank(cNew.getTitle())) {
                mods.add(new Modification(ModificationType.DELETE, ATTR_TITLE));
            } else if (!StringUtils.equals(cOld.getTitle(), cNew.getTitle())) {
                mods.add(new Modification(ModificationType.REPLACE, ATTR_TITLE, cNew.getTitle()));
            }
        }
    }

    private void checkForEnabledStatusModification(Application cOld, Application cNew, List<Modification> mods) {
        if (cNew.getEnabled() != null && !cNew.getEnabled().equals(cOld.getEnabled())) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_ENABLED, String.valueOf(cNew.getEnabled()).toUpperCase()));
        }
    }

    void checkForUseForDefaultRegionModification(Application cOld, Application cNew, List<Modification> mods) {
        if (cNew.getUseForDefaultRegion() != null && !cNew.getUseForDefaultRegion().equals(cOld.getUseForDefaultRegion())) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_USE_FOR_DEFAULT_REGION, String.valueOf(cNew.getUseForDefaultRegion()).toUpperCase()));
        }
    }

    private void checkForClientSecretModification(Application cNew, CryptHelper cryptHelper, List<Modification> mods) throws GeneralSecurityException, InvalidCipherTextException {
        //TODO null pointer?

        String versionId = getEncryptionVersionId(cNew);
        String salt = getSalt(cNew);

        if (cNew.getClientSecretObj() != null && cNew.getClientSecretObj().isNew()) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_CLIENT_SECRET, cNew.getClientSecretObj().getValue()));
            mods.add(new Modification(ModificationType.REPLACE, ATTR_CLEAR_PASSWORD, cryptHelper.encrypt(
                    cNew.getClientSecretObj().getValue(), versionId, salt)));
        }
    }

    private void checkForRCNModification(Application cOld, Application cNew, List<Modification> mods) {
        if (cNew.getRcn() != null && !cNew.getRcn().equals(cOld.getRcn())) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_RACKSPACE_CUSTOMER_NUMBER, cNew.getRcn()));
        }
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
        return getObject(searchFilterGetApplicationByClientId(id), getSoftDeletedBaseDn());
    }

    @Override
    public Application getSoftDeletedClientByName(String clientName) {

        getLogger().debug("Doing search for application " + clientName);
        if (StringUtils.isBlank(clientName)) {
            getLogger().error("Null or Empty clientName parameter");
            throw new IllegalArgumentException(
                    "Null or Empty clientName parameter.");
        }

        Filter searchFilter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_NAME, clientName)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEAPPLICATION)
                .build();

        Application application = getSingleSoftDeletedClient(searchFilter);

        getLogger().debug("Found Application - {}", application);

        return application;
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
}
