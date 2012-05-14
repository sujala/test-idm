package com.rackspace.idm.api.resource.cloud.migration;

import org.openstack.docs.identity.api.v2.UserList;

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups;
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.docs.identity.api.ext.rax_ksqa.v1.SecretQA;
import com.rackspace.idm.api.converter.cloudv20.EndpointConverterCloudV20;
import com.rackspace.idm.api.converter.cloudv20.RoleConverterCloudV20;
import com.rackspace.idm.api.converter.cloudv20.UserConverterCloudV20;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.api.resource.cloud.MigrationClient;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.NotAuthenticatedException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspacecloud.docs.auth.api.v1.BaseURL;
import com.rackspacecloud.docs.auth.api.v1.BaseURLList;
import com.sun.jersey.api.ConflictException;
import com.sun.servicetag.UnauthorizedAccessException;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpException;
import org.joda.time.DateTime;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplateList;
import org.openstack.docs.identity.api.v2.*;
import org.openstack.docs.identity.api.v2.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: matt.colton
 * Date: 4/9/12
 * Time: 2:56 PM
 * To change this template use File | Settings | File Templates.
 */

@Component
public class CloudMigrationService {

    private MigrationClient client;

    @Autowired
    private UserService userService;
    
    @Autowired
    private TenantService tenantService;
    
    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private ScopeAccessService scopeAccessService;

    @Autowired
    private EndpointService endpointService;

    @Autowired
    private Configuration config;

    @Autowired
    private GroupService cloudGroupService;

    @Autowired
    private JAXBObjectFactories OBJ_FACTORIES;

    @Autowired
    private UserConverterCloudV20 userConverterCloudV20;
    
    @Autowired
    private RoleConverterCloudV20 roleConverterCloudV20;

    @Autowired
    private EndpointConverterCloudV20 endpointConverterCloudV20;

    final private Logger logger = LoggerFactory.getLogger(this.getClass());


    public void migrateBaseURLs() throws Exception {
        addOrUpdateEndpointTemplates(getAdminToken());
    }

    public Response.ResponseBuilder getMigratedUserList() throws Exception {
        FilterParam[] filters = new FilterParam[] { new FilterParam(FilterParam.FilterParamName.MIGRATED, null)};
        com.rackspace.idm.domain.entity.Users users = userService.getAllUsers(filters);
        if(users == null)
            throw new NotFoundException("Users not found.");
        return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory().createUsers(userConverterCloudV20.toUserList(users.getUsers())));
    }

    public Response.ResponseBuilder getInMigrationUserList() throws Exception {
        FilterParam[] filters = new FilterParam[] { new FilterParam(FilterParam.FilterParamName.IN_MIGRATION, null)};
        com.rackspace.idm.domain.entity.Users users = userService.getAllUsers(filters);
        if(users == null)
            throw new NotFoundException("Users not found.");
        return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory().createUsers(userConverterCloudV20.toUserList(users.getUsers())));
    }

    public Response.ResponseBuilder getMigratedUser(String username) throws Exception {
        com.rackspace.idm.domain.entity.User user = userService.getUser(username);
        if(user == null)
            throw new NotFoundException("User not found.");
        return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory().createUser(userConverterCloudV20.toUser(user)));
    }

    public Response.ResponseBuilder getMigratedUserRoles(String username) throws Exception {
        com.rackspace.idm.domain.entity.User user = userService.getUser(username);
        if(user == null)
            throw new NotFoundException("User not found.");
        List<TenantRole> roles = tenantService.getTenantRolesForUser(user, null);
        return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory().createRoles(roleConverterCloudV20.toRoleListJaxb(roles)));
    }

    public Response.ResponseBuilder getMigratedUserEndpoints(String username) throws Exception {
        com.rackspace.idm.domain.entity.User user = userService.getUser(username);
        if(user == null)
            throw new NotFoundException("User not found.");
        ScopeAccess sa = scopeAccessService.getUserScopeAccessForClientId(user.getUniqueId(), config.getString("cloudAuth.clientId"));
        List<OpenstackEndpoint> endpoints = scopeAccessService.getOpenstackEndpointsForScopeAccess(sa);
        EndpointList list = endpointConverterCloudV20.toEndpointList(endpoints);
        return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory().createEndpoints(list));
    }

    public String migrateUserByUsername(String username, boolean enable, String domainId) throws Exception {
        client = new MigrationClient();
		client.setCloud20Host(config.getString("cloudAuth20url"));

        if(userService.userExistsByUsername(username))
            throw new ConflictException("A user with username "+ username +" already exists.");

        String adminToken = getAdminToken();

        if(!adminToken.equals("")) {
            User user;
            try {
                user = client.getUser(adminToken, username);
            }
            catch(Exception ex) {
                throw new NotFoundException("User with username "+ username +" could not be found.");
            }
            String legacyId = user.getId();

            CredentialListType credentialListType = client.getUserCredentials(adminToken, user.getId());
            credentialListType.getCredential();

            String apiKey = getApiKey(credentialListType);
            String password = getPassword(credentialListType);
            
            if(password.equals(""))
                password = Password.generateRandom(false).getValue();

            // Get Secret QA
            SecretQA secretQA = getSecretQA(adminToken, user.getId());

            // Set null so a new ID is given since ID exists.
            if(userService.userExistsById(user.getId()))
                user.setId(null);

            // CREATE NEW USER
            com.rackspace.idm.domain.entity.User newUser = addMigrationUser(user, apiKey, password, secretQA, domainId);

            AuthenticateResponse authenticateResponse = authenticate(username, apiKey, password);

            UserForAuthenticateResponse cloudUser = authenticateResponse.getUser();
            String userToken = authenticateResponse.getToken().getId();

            // Get Roles
            addUserGlobalRoles(newUser, cloudUser.getRoles());

            // Get Tenants
            addTenantsForUserByToken(newUser, adminToken, userToken);

            // Groups
            addUserGroups(adminToken, user.getId(), legacyId);

            if(enable){
                newUser.setInMigration(false);
                userService.updateUserById(newUser, false);
            }

            if (isUserAdmin(cloudUser)) {
                UserList users = null;

                try {
                    users = client.getUsers(userToken);
                } catch (JAXBException e) {
                }

                if (users != null) {
                    for (User childUser : users.getUser()) {
                        if (newUser.getUsername().equalsIgnoreCase(childUser.getUsername())) {
                            continue;
                        }
                        migrateUserByUsername(childUser.getUsername(), enable, newUser.getDomainId());
                    }
                }
            }

            return newUser.getId();
        }
        throw new UnauthorizedAccessException("Not Authorized.");
    }

	private String getApiKey(CredentialListType credentialListType) {
		String apiKey = "";
		
		try {
		    List<JAXBElement<? extends CredentialType>> creds = credentialListType.getCredential();
		    for(JAXBElement<? extends CredentialType> cred : creds){
		        if(cred.getDeclaredType().isAssignableFrom(ApiKeyCredentials.class)){
		            ApiKeyCredentials apiKeyCredentials = (ApiKeyCredentials)cred.getValue();
		            apiKey = apiKeyCredentials.getApiKey();
		        }
		    }
		} catch (Exception cex){
		    
		}
		return apiKey;
	}

	private String getPassword(CredentialListType credentialListType) {
		String password = "";

		try {
		    List<JAXBElement<? extends CredentialType>> creds = credentialListType.getCredential();
		    for(JAXBElement<? extends CredentialType> cred : creds){
		        if(cred.getDeclaredType().isAssignableFrom(PasswordCredentialsRequiredUsername.class)){
		            PasswordCredentialsRequiredUsername passwordCredentials = (PasswordCredentialsRequiredUsername)cred.getValue();
		            password = passwordCredentials.getPassword();
		        }
		    }
		} catch (Exception cex){
		    
		}
		return password;
	}

	private AuthenticateResponse authenticate(String username, String apiKey,
			String password) throws Exception {
		AuthenticateResponse authenticateResponse;
		try {
		    if(!apiKey.equals(""))
		        authenticateResponse = client.authenticateWithApiKey(username, apiKey);
		    else if(!password.equals(""))
		        authenticateResponse = client.authenticateWithPassword(username, password);
		    else {
		        throw new BadRequestException("Failed migration with incomplete credential information.");
		    }
		}
		catch(Exception ex) {
		    unmigrateUserByUsername(username);
		    throw new BadRequestException("Failed migration with incomplete credential information.");
		}
		return authenticateResponse;
	}

	private boolean isUserAdmin(UserForAuthenticateResponse cloudUser) {
		for (Role role : cloudUser.getRoles().getRole()) {
		    if ("identity:user-admin".equalsIgnoreCase(role.getName())) {
		        return true;
		    }
		}
		return false;
	}

    public void setMigratedUserEnabledStatus(String username, boolean enable) {
        com.rackspace.idm.domain.entity.User user = userService.getUser(username);
        if(user == null)
            throw new NotFoundException("User not found.");
        if(user.getInMigration() == null)
            throw new NotFoundException("User not found.");
        user.setInMigration(enable);
        userService.updateUserById(user, false);
    }

    public void unmigrateUserByUsername(String username) throws Exception {
        com.rackspace.idm.domain.entity.User user = userService.getUser(username);
        if(user == null)
            throw new NotFoundException("User not found.");
        if(user.getInMigration() == null) // Used so we do not delete a user who wasn't previously migrated.
            throw new NotFoundException("User not found.");

        String adminToken = getAdminToken();
        
        CredentialListType credentialListType = client.getUserCredentials(adminToken, user.getId());
        String apiKey = getApiKey(credentialListType);
        String password = getPassword(credentialListType);
        
        AuthenticateResponse authenticateResponse = authenticate(username, apiKey, password);
        UserForAuthenticateResponse cloudUser = authenticateResponse.getUser();
        String userToken = authenticateResponse.getToken().getId();

        if (isUserAdmin(cloudUser)) {
            UserList users = null;

            try {
                users = client.getUsers(userToken);
            } catch (JAXBException e) {
            }

            if (users != null) {
                for (User childUser : users.getUser()) {
                    if (user.getUsername().equalsIgnoreCase(childUser.getUsername())) {
                        continue;
                    }
                    try {
                        unmigrateUserByUsername(childUser.getUsername());
                    } catch (Exception e) {
                    }
                }
            }
        }

        userService.deleteUser(username);
    }
    
    private String getAdminToken() throws URISyntaxException, HttpException, IOException, JAXBException {
        try {
            client = new MigrationClient();
            client.setCloud20Host(config.getString("cloudAuth20url"));
            String adminUsername = config.getString("migration.username");
            String adminApiKey = config.getString("migration.apikey");
            AuthenticateResponse authenticateResponse = client.authenticateWithApiKey(adminUsername, adminApiKey);
            return authenticateResponse.getToken().getId();
        }
        catch(Exception ex) {
            throw new NotAuthenticatedException("Admin credentials are invalid");
        }
	}
    
    private com.rackspace.idm.domain.entity.User addMigrationUser(User user, String apiKey, String password, SecretQA secretQA, String domainId) {
        com.rackspace.idm.domain.entity.User newUser = new com.rackspace.idm.domain.entity.User();
        newUser.setId(user.getId());
        newUser.setUsername(user.getUsername());

        if(!user.getEmail().equals(""))
            newUser.setEmail(user.getEmail());

        newUser.setApiKey(apiKey);
        newUser.setPassword(password);
        newUser.setEnabled(true);
        newUser.setCreated(new DateTime(user.getCreated().toGregorianCalendar().getTime()));

        if(user.getUpdated() != null)
            newUser.setUpdated(new DateTime(user.getUpdated().toGregorianCalendar().getTime()));

        if(secretQA != null) {
            newUser.setSecretQuestion(secretQA.getQuestion());
            newUser.setSecretAnswer(secretQA.getAnswer());
        }

        newUser.setInMigration(true);
        newUser.setMigrationDate(new DateTime());

        if (domainId != null) {
            newUser.setDomainId(domainId);
        }

        userService.addUser(newUser);
        return newUser;
    }

    private void addUserGroups(String adminToken, String userId, String legacyId) throws Exception {
        try {
            Groups groups = client.getGroupsForUser(adminToken, legacyId);
            for(Group group : groups.getGroup()){
                cloudGroupService.addGroupToUser(Integer.parseInt(group.getId()), userId);
            }
        }catch(Exception ex){
            // TODO: what to do?
        }
    }

    private void addUserGlobalRoles(com.rackspace.idm.domain.entity.User user, RoleList roleList) {

        List<ClientRole> clientRoles = applicationService.getAllClientRoles(null);
        for (Role role : roleList.getRole()) {
            for(ClientRole cRole : clientRoles) {
                if(cRole.getName().equals(role.getName())) {
                    TenantRole newRole = new TenantRole();
                    newRole.setName(cRole.getName());
                    newRole.setClientId(cRole.getClientId());
                    newRole.setDescription(cRole.getDescription());
                    newRole.setRoleRsId(cRole.getId());
                    newRole.setUserId(user.getId());
                    tenantService.addTenantRoleToUser(user, newRole);
                }
            }
        }
    }

    private void addTenantsForUserByToken(com.rackspace.idm.domain.entity.User user, String adminToken, String token) throws Exception {
        client = new MigrationClient();
		client.setCloud20Host(config.getString("cloudAuth20url"));
        client.setCloud11Host(config.getString("cloudAuth11url"));
        // Using Endpoints call to get both Tenants and Endpoint information
        EndpointList endpoints = client.getEndpointsByToken(adminToken, token);
        if(endpoints != null) {
            for (Endpoint endpoint : endpoints.getEndpoint()) {
                // Add the Tenant if it doesn't exist.
                com.rackspace.idm.domain.entity.Tenant newTenant = tenantService.getTenant(endpoint.getTenantId());
                if (newTenant == null) {
                    // Find all BaseUrls
                    String[] baseUrls = getBaseUrlsForTenant(endpoint.getTenantId(), endpoints);
                    // Add new Tenant
                    addTenant(endpoint, baseUrls);
                }
                // Add roles to user on tenant
                addTenantRole(user, endpoint);
            }
        }
    }

    private void addTenant(Endpoint endpoint, String [] baseUrls) {
        com.rackspace.idm.domain.entity.Tenant newTenant = new com.rackspace.idm.domain.entity.Tenant();
        newTenant.setTenantId(endpoint.getTenantId());
        newTenant.setName(endpoint.getTenantId());
        newTenant.setEnabled(true);
        newTenant.setBaseUrlIds(baseUrls);
        tenantService.addTenant(newTenant);
    }

    private void addTenantRole(com.rackspace.idm.domain.entity.User user, Endpoint endpoint) {
        Application application = applicationService.getByName(endpoint.getName());
        if(application == null) {
            logger.debug("Unknown application detected");
            // ToDo: Should we throw an error and roll everything back?
        }
        else {
            List<ClientRole> clientRoles = applicationService.getClientRolesByClientId(application.getClientId());

            for(ClientRole role : clientRoles){
                TenantRole tenantRole = new TenantRole();
                tenantRole.setClientId(application.getClientId());
                tenantRole.setUserId(user.getId());
                tenantRole.setTenantIds(new String[]{endpoint.getTenantId()}); //ToDo: does this overwrite a previous?
                tenantRole.setName(role.getName());
                tenantRole.setRoleRsId(role.getId());
                tenantService.addTenantRoleToUser(user, tenantRole);
            }
        }
    }

    private String[] getBaseUrlsForTenant(String tenantId, EndpointList endpoints) {
        List<String> baseUrls = new ArrayList<String>();
        for (Endpoint endpoint : endpoints.getEndpoint()) {
            if(endpoint.getTenantId().equals(tenantId))
                baseUrls.add(String.valueOf(endpoint.getId()));
        }
        String[] strResult = new String[baseUrls.size()];
        return baseUrls.toArray(strResult);
    }

    private SecretQA getSecretQA(String adminToken, String userId) throws Exception {
        try {
            client.setCloud20Host(config.getString("cloudAuth20url"));
            SecretQA secretQA = client.getSecretQA(adminToken, userId);
            return secretQA;
        }
        catch(Exception ex) {
            return null;
        }
    }

    private void addOrUpdateEndpointTemplates(String adminToken) throws Exception {
        // Using Endpoints call to get Keystone Endpoint
        client.setCloud20Host(config.getString("cloudAuth20url"));
        EndpointTemplateList endpoints = client.getEndpointTemplates(adminToken);

        //Get V1.1 BaseURLs for extra info
        BaseURLList baseURLs;
        try {
            client = new MigrationClient();
            client.setCloud11Host(config.getString("cloudAuth11url"));
            baseURLs = client.getBaseUrls(config.getString("ga.username"), config.getString("ga.password"));
        }
        catch(Exception ex){
            baseURLs = new BaseURLList();
        }
        if(endpoints != null) {
            for (EndpointTemplate endpoint : endpoints.getEndpointTemplate()) {
                CloudBaseUrl cloudBaseUrl = endpointService.getBaseUrlById(endpoint.getId());

                BaseURL baseURL = getBaseUrlFromEndpoint(endpoint.getId(), baseURLs.getBaseURL());

                if(cloudBaseUrl == null){
                    cloudBaseUrl = copyCloudBaseUrlFromEndpointTemplate(endpoint);

                    //Use V1.1 to add additional info
                    if(baseURL != null) {
                        cloudBaseUrl.setBaseUrlType(baseURL.getUserType().value());
                        cloudBaseUrl.setDef(baseURL.isDefault());
                    }
                    else{
                        cloudBaseUrl.setBaseUrlType("MOSSO");
                        cloudBaseUrl.setDef(false);
                    }
                    endpointService.addBaseUrl(cloudBaseUrl);
                }
                else {
                    String uid = cloudBaseUrl.getUniqueId();
                    cloudBaseUrl = copyCloudBaseUrlFromEndpointTemplate(endpoint);
                    cloudBaseUrl.setUniqueId(uid);

                    //Use V1.1 to add additional info
                    if(baseURL != null) {
                        cloudBaseUrl.setBaseUrlType(baseURL.getUserType().value());
                        cloudBaseUrl.setDef(baseURL.isDefault());
                    }
                    else{
                        cloudBaseUrl.setBaseUrlType("MOSSO");
                        cloudBaseUrl.setDef(false);
                    }
                    
                    endpointService.updateBaseUrl(cloudBaseUrl);
                }
            }
        }
    }

    private CloudBaseUrl copyCloudBaseUrlFromEndpointTemplate(EndpointTemplate endpoint) {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setBaseUrlId(endpoint.getId());
        cloudBaseUrl.setAdminUrl(endpoint.getAdminURL());
        cloudBaseUrl.setEnabled(endpoint.isEnabled());
        cloudBaseUrl.setGlobal(endpoint.isGlobal());
        cloudBaseUrl.setInternalUrl(endpoint.getInternalURL());
        cloudBaseUrl.setOpenstackType(endpoint.getType());

        if(!StringUtils.isBlank(endpoint.getPublicURL()))
            cloudBaseUrl.setPublicUrl(endpoint.getPublicURL());
        else
            cloudBaseUrl.setPublicUrl("https://");

        cloudBaseUrl.setServiceName(endpoint.getName());
        cloudBaseUrl.setRegion(endpoint.getRegion());
        cloudBaseUrl.setVersionId(endpoint.getVersion().getId());
        cloudBaseUrl.setVersionInfo(endpoint.getVersion().getInfo());
        cloudBaseUrl.setVersionList(endpoint.getVersion().getList());
        return cloudBaseUrl;
    }

    private BaseURL getBaseUrlFromEndpoint(int endpointId, List<BaseURL> baseURLs) {
        for(BaseURL b : baseURLs){
            if(b.getId() == endpointId)
                return b;
        }
        return null;
    }
    
    private void addUserGroups(String userId, Groups groups) throws Exception {
        try {
            for (Group group : groups.getGroup()) {
                String xxx = group.getId();
            }
        } catch(Exception gex) {

        }
    }

    public MigrationClient getClient() {
        return client;
    }

    public void setClient(MigrationClient client) {
        this.client = client;
    }

    public UserService getUserService() {
        return userService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public TenantService getTenantService() {
        return tenantService;
    }

    public void setTenantService(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    public ApplicationService getApplicationService() {
        return applicationService;
    }

    public void setApplicationService(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    public EndpointService getEndpointService() {
        return endpointService;
    }

    public void setEndpointService(EndpointService endpointService) {
        this.endpointService = endpointService;
    }

    public void setCloudGroupService(GroupService cloudGroupService) {
        this.cloudGroupService = cloudGroupService;
    }

    public ScopeAccessService getScopeAccessService() {
        return scopeAccessService;
    }

    public void setScopeAccessService(ScopeAccessService scopeAccessService) {
        this.scopeAccessService = scopeAccessService;
    }

    public Configuration getConfig() {
        return config;
    }

    public void setConfig(Configuration config) {
        this.config = config;
    }

    public JAXBObjectFactories getOBJ_FACTORIES() {
        return OBJ_FACTORIES;
    }

    public void setOBJ_FACTORIES(JAXBObjectFactories OBJ_FACTORIES) {
        this.OBJ_FACTORIES = OBJ_FACTORIES;
    }

    public UserConverterCloudV20 getUserConverterCloudV20() {
        return userConverterCloudV20;
    }

    public void setUserConverterCloudV20(UserConverterCloudV20 userConverterCloudV20) {
        this.userConverterCloudV20 = userConverterCloudV20;
    }

    public RoleConverterCloudV20 getRoleConverterCloudV20() {
        return roleConverterCloudV20;
    }

    public void setRoleConverterCloudV20(RoleConverterCloudV20 roleConverterCloudV20) {
        this.roleConverterCloudV20 = roleConverterCloudV20;
    }

    public EndpointConverterCloudV20 getEndpointConverterCloudV20() {
        return endpointConverterCloudV20;
    }

    public void setEndpointConverterCloudV20(EndpointConverterCloudV20 endpointConverterCloudV20) {
        this.endpointConverterCloudV20 = endpointConverterCloudV20;
    }
}
