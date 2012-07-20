package com.rackspace.idm.api.resource.cloud.migration;

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups;
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.docs.identity.api.ext.rax_ksqa.v1.SecretQA;
import com.rackspace.idm.api.converter.cloudv20.EndpointConverterCloudV20;
import com.rackspace.idm.api.converter.cloudv20.RoleConverterCloudV20;
import com.rackspace.idm.api.converter.cloudv20.UserConverterCloudV20;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.api.resource.cloud.MigrationClient;
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperClient;
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperConstants;
import com.rackspace.idm.api.resource.cloud.v20.CloudKsGroupBuilder;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.NotAuthenticatedException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspacecloud.docs.auth.api.v1.BaseURL;
import com.rackspacecloud.docs.auth.api.v1.BaseURLList;
import com.rackspacecloud.docs.auth.api.v1.BaseURLRef;
import com.sun.jersey.api.ConflictException;
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
import javax.xml.namespace.QName;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
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

    private static final int UK_BASEURL_OFFSET = 1000;
    
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
    private JAXBObjectFactories obj_factories;

    @Autowired
    private UserConverterCloudV20 userConverterCloudV20;

    @Autowired
    private RoleConverterCloudV20 roleConverterCloudV20;

    @Autowired
    private EndpointConverterCloudV20 endpointConverterCloudV20;

    @Autowired
    private CloudKsGroupBuilder cloudKsGroupBuilder;

    @Autowired
    private AtomHopperClient atomHopperClient;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());


    public CloudMigrationService() {
        client = new MigrationClient();
    }

    public void migrateBaseURLs(){
        try {
            addOrUpdateEndpointTemplates(getAdminToken());
        }
        catch (Exception ex) {

        }
    }

    public void migrateRoles() {
        RoleList roles;
        try {
            String adminToken = getAdminToken();
            roles = client.getRoles(adminToken);
        }
        catch (Exception ex) {
            roles = new RoleList();
        }

        for (Role role : roles.getRole()) {
            ClientRole clientRole = applicationService.getClientRoleById(role.getId());

            if (clientRole == null) {
                clientRole = new ClientRole();
                clientRole.setId(role.getId());
                clientRole.setDescription(role.getDescription());
                clientRole.setName(role.getName());
                clientRole.setClientId(config.getString("cloudAuth.clientId"));

                applicationService.addClientRole(clientRole, clientRole.getId());
            } else { 
                if (!role.getDescription().equals(clientRole.getDescription()) ||
                    !role.getName().equals(clientRole.getName())) {
                    clientRole.setDescription(role.getDescription());
                    clientRole.setName(role.getName());

                    applicationService.updateClientRole(clientRole);
                }
            }
        }
    }

    public void migrateGroups() throws Exception {
        if(isUkCloudRegion()) {
            throw new NotFoundException("Method not found.");
        }
        addOrUpdateGroups(getAdminToken());
    }

    public Response.ResponseBuilder getGroups() {
        List<com.rackspace.idm.domain.entity.Group> groups = cloudGroupService.getGroups("", 0);

        com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups cloudGroups = new com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups();

        for (com.rackspace.idm.domain.entity.Group group : groups) {
            com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group cloudGroup = cloudKsGroupBuilder.build(group);
            cloudGroups.getGroup().add(cloudGroup);
        }

        return Response.ok(obj_factories.getRackspaceIdentityExtKsgrpV1Factory().createGroups(cloudGroups));
    }

    public Response.ResponseBuilder getMigratedUserList() throws Exception {
        FilterParam[] filters = new FilterParam[]{new FilterParam(FilterParam.FilterParamName.MIGRATED, null)};
        com.rackspace.idm.domain.entity.Users users = userService.getAllUsers(filters);
        if (users == null)
            throw new NotFoundException("Users not found.");
        return Response.ok(obj_factories.getOpenStackIdentityV2Factory().createUsers(userConverterCloudV20.toUserList(users.getUsers())));
    }

    public Response.ResponseBuilder getInMigrationUserList() throws Exception {
        FilterParam[] filters = new FilterParam[]{new FilterParam(FilterParam.FilterParamName.IN_MIGRATION, null)};
        com.rackspace.idm.domain.entity.Users users = userService.getAllUsers(filters);
        if (users == null)
            throw new NotFoundException("Users not found.");
        return Response.ok(obj_factories.getOpenStackIdentityV2Factory().createUsers(userConverterCloudV20.toUserList(users.getUsers())));
    }

    public Response.ResponseBuilder getMigratedUser(String username) throws Exception {
        com.rackspace.idm.domain.entity.User user = userService.getUser(username);
        if (user == null)
            throw new NotFoundException("User not found.");
        return Response.ok(obj_factories.getOpenStackIdentityV2Factory().createUser(userConverterCloudV20.toUser(user)));
    }

    public Response.ResponseBuilder getMigratedUserRoles(String username) throws Exception {
        com.rackspace.idm.domain.entity.User user = userService.getUser(username);
        if (user == null)
            throw new NotFoundException("User not found.");
        List<TenantRole> roles = tenantService.getGlobalRolesForUser(user);
        return Response.ok(obj_factories.getOpenStackIdentityV2Factory().createRoles(roleConverterCloudV20.toRoleListJaxb(roles)));
    }

    public Response.ResponseBuilder getMigratedUserEndpoints(String username) throws Exception {
        com.rackspace.idm.domain.entity.User user = userService.getUser(username);
        if (user == null)
            throw new NotFoundException("User not found.");
        EndpointList list = getEndpointsForUser(user.getUniqueId());
        return Response.ok(obj_factories.getOpenStackIdentityV2Factory().createEndpoints(list));
    }

    EndpointList getEndpointsForUser(String userId) {
        ScopeAccess sa = scopeAccessService.getUserScopeAccessForClientId(userId, config.getString("cloudAuth.clientId"));
        List<OpenstackEndpoint> endpoints = scopeAccessService.getOpenstackEndpointsForScopeAccess(sa);
        EndpointList list = endpointConverterCloudV20.toEndpointList(endpoints);
        return list;
    }


    public MigrateUserResponseType migrateUserByUsername(String username, boolean processSubUsers) throws Exception {
        try {
            MigrateUserResponseType response = migrateUserByUsername(username, processSubUsers, null);

            //Feed to atom hopper for migrated users
            List<com.rackspace.idm.api.resource.cloud.migration.UserType> users = response.getUsers();
            List<com.rackspace.idm.domain.entity.User> migratedUsers = new ArrayList<com.rackspace.idm.domain.entity.User>();
            for (com.rackspace.idm.api.resource.cloud.migration.UserType migratedUser : users) {
                com.rackspace.idm.domain.entity.User tempUser = userService.getUser(migratedUser.getUsername());
                migratedUsers.add(tempUser);
            }
            String adminToken = getAdminToken();
            for (com.rackspace.idm.domain.entity.User migUser : migratedUsers) {
                atomHopperClient.asyncPost(migUser, adminToken, AtomHopperConstants.MIGRATED, AtomHopperConstants.SUCCESS);
            }

            return response;
        } catch (ConflictException ce){
            throw ce;
        } catch (Exception e) {
            try {
                unmigrateUserByUsername(username);
            } catch (Exception e2) {
            }
            throw e;
        }
    }

    public MigrateUserResponseType migrateUserByUsername(String username, boolean processSubUsers, String domainId) throws Exception {
        client.setCloud20Host(config.getString("cloudAuth20url"));
        client.setCloud11Host(config.getString("cloudAuth11url"));
        if (userService.userExistsByUsername(username)) {
            throw new ConflictException("A user with username " + username + " already exists.");
        }

        String adminToken = getAdminToken();

        if (!adminToken.equals("")) {
            com.rackspacecloud.docs.auth.api.v1.User user11;
            User user;
            try {
                user = client.getUser(adminToken, username);
                user11 = client.getUserTenantsBaseUrls(config.getString("ga.username"), config.getString("ga.password"), username);
            }
            catch (Exception ex) {
                throw new NotFoundException("User with username " + username + " could not be found.");
            }
            String legacyId = user.getId();

            CredentialListType credentialListType = null;
            String apiKey = "";
            String cloudPassword = "";
            try {
                credentialListType = client.getUserCredentials(adminToken, user.getId());
                credentialListType.getCredential();
                apiKey = getApiKey(credentialListType);
                cloudPassword = getPassword(credentialListType);
            }
            catch (Exception exCred) {
                // User does not have crednetials
            }
            String password = cloudPassword;

            if (password.equals("")) {
                password = Password.generateRandom(false).getValue();
            }

            RoleList roles = client.getRolesForUser(adminToken, user.getId());

            if (domainId == null && isSubUser(roles)) {
                throw new BadRequestException("Migration is not allowed for subusers");
            }


            String userToken = "";
            Date expireToken = new Date();
            try {
                AuthenticateResponse authenticateResponse = authenticate(username, apiKey, password);
                userToken = authenticateResponse.getToken().getId();
                expireToken = authenticateResponse.getToken().getExpires().toGregorianCalendar().getTime();
            }catch (Exception e){
                userToken = "";
            }

            List<String> subUsers = null;
            if(processSubUsers) {
                subUsers = getSubUsers(user, userToken, roles);
                for (String subUser : subUsers) {
                    if (userService.userExistsByUsername(subUser)) {
                        throw new ConflictException("A user with username " + subUser + " already exists.");
                    }
                }
            }

            // Get Secret QA
            SecretQA secretQA = getSecretQA(adminToken, user.getId());

            // Set null so a new ID is given since ID exists.
            if (userService.userExistsById(user.getId())) {
                user.setId(null);
            }

            // CREATE NEW USER
            com.rackspace.idm.domain.entity.User newUser = addMigrationUser(user, user11.getMossoId(),
                    user11.getNastId(), apiKey, password, secretQA, domainId);

            //Add CA token to GA User
            if(!userToken.equals("")) {
                try {
                    scopeAccessService.updateUserScopeAccessTokenForClientIdByUser(newUser, config.getString("cloudAuth.clientId"), userToken, expireToken);
                }catch(Exception e){

                }
            }
            
            // Get Roles
            addUserGlobalRoles(newUser, roles);

            // Get Tenants
            List<String> mossoBaseUrlRef = new ArrayList<String>();
            List<String> nastBaseUrlRef = new ArrayList<String>();

            for (BaseURLRef baseUrlRef : user11.getBaseURLRefs().getBaseURLRef()) {
                CloudBaseUrl cloudBaseUrl = endpointService.getBaseUrlById(baseUrlRef.getId());

                int baseUrlId = baseUrlRef.getId();
                if (isUkCloudRegion()) {
                    baseUrlId += UK_BASEURL_OFFSET;
                }

                if ("MOSSO".equals(cloudBaseUrl.getBaseUrlType())) {
                    mossoBaseUrlRef.add(String.valueOf(baseUrlId));
                }

                if ("NAST".equals(cloudBaseUrl.getBaseUrlType())) {
                    nastBaseUrlRef.add(String.valueOf(baseUrlId));
                }
            }

            if(user11.getMossoId() != null) {
                addTenantsForUserByToken(newUser, user11.getMossoId().toString(), mossoBaseUrlRef);
            }
            if(user11.getNastId() != null) {
                addTenantsForUserByToken(newUser, user11.getNastId(), nastBaseUrlRef);
            }

            // Groups
            Groups groups = client.getGroupsForUser(adminToken, legacyId);
            addUserGroups(user.getId(), groups);

            newUser.setInMigration(false);
            userService.updateUserById(newUser, false);

            UserType userResponse = validateUser(user, apiKey, cloudPassword, secretQA, roles, groups, user11.getBaseURLRefs().getBaseURLRef());
            MigrateUserResponseType result = new MigrateUserResponseType();

            if(subUsers != null) {
                for (String subUser : subUsers) {
                    MigrateUserResponseType childResponse = migrateUserByUsername(subUser, false, newUser.getDomainId());
                    result.getUsers().addAll(childResponse.getUsers());
                }
            }

            result.getUsers().add(userResponse);

            return result;
        }
        throw new NotAuthenticatedException("Not Authorized.");
    }

	String getDefaultRegion(User user) {
		String defaultRegion = null;
		QName defaultRegionQName = new QName("http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0","defaultRegion");
		for (QName qname : user.getOtherAttributes().keySet()) {
		    if (qname.equals(defaultRegionQName)) {
		        defaultRegion = user.getOtherAttributes().get(qname);
		    }
		}
		return defaultRegion;
	}

    List<String> getSubUsers(User user, String userToken, RoleList roles) throws Exception {
        client.setCloud20Host(config.getString("cloudAuth20url"));
        client.setCloud11Host(config.getString("cloudAuth11url"));
        List<String> subUsers = new ArrayList<String>();

        if (isUserAdmin(roles)) {
            if (!user.isEnabled()) {
                throw new ConflictException("useradmin with username " + user.getUsername() + " is disabled.");
            }



            UserList users = null;

            try {
                users = client.getUsers(userToken);
            } catch (Exception e) {
                throw new ConflictException("Could not retrieve users for useradmin - " + e.getMessage());
            }

            if (users != null) {
                for (User childUser : users.getUser()) {
                    if (StringUtils.equalsIgnoreCase(user.getUsername(), childUser.getUsername())) {
                        continue;
                    }
                    subUsers.add(childUser.getUsername());
                }
            }
        }

        return subUsers;
    }

    UserType validateUser(User user, String apiKey, String password, SecretQA secretQA, RoleList newRoles, Groups groups, List<BaseURLRef> baseUrlRefs) {

        UserType result = new UserType();
        com.rackspace.idm.domain.entity.User newUser = userService.getUser(user.getUsername());

        List<String> commentList = new ArrayList<String>();

        result.setId(newUser.getId());
        result.setUsername(newUser.getUsername());
        result.setEmail(newUser.getEmail());

        result.setApiKey("*****");         //newUser.getApiKey());
        result.setPassword("*****");       //newUser.getPassword());
        result.setSecretQuestion("*****"); //newUser.getSecretQuestion());
        result.setSecretAnswer("*****");   //newUser.getSecretAnswer());

        checkIfEqual(user.getId(), newUser.getId(), commentList, "id");
        checkIfEqual(user.getEmail(), newUser.getEmail(), commentList, "email");
        checkIfEqual(apiKey, newUser.getApiKey(), commentList, "apiKey", true);
        checkIfEqual(password, newUser.getPassword(), commentList, "password", true);

        if (secretQA != null) {
            checkIfEqual(secretQA.getQuestion(), newUser.getSecretQuestion(), commentList, "secretQuestion", true);
            checkIfEqual(secretQA.getAnswer(), newUser.getSecretAnswer(), commentList, "secretAnswer", true);
        }

        String comment = StringUtils.join(commentList, ",");

        result.setComment(comment);
        result.setValid(StringUtils.isBlank(comment));

        List<TenantRole> roles = tenantService.getGlobalRolesForUser(newUser);
        validateRoles(roles, newRoles, result);

        List<com.rackspace.idm.domain.entity.Group> newGroups = cloudGroupService.getGroupsForUser(newUser.getId());
        validateGroups(groups, newGroups, result);

        EndpointList newEndpoints = getEndpointsForUser(newUser.getUniqueId());
        validateBaseUrlRefs(baseUrlRefs, newEndpoints, result);

        return result;
    }

    void validateBaseUrlRefs(List<BaseURLRef> baseUrlRefs, EndpointList newEndpoints, UserType result) {
        List<String> commentList;

        for (BaseURLRef baseUrlRef : baseUrlRefs) {
            commentList = new ArrayList<String>();

            String newEndpointId = null;

            for (Endpoint newEndpoint : newEndpoints.getEndpoint()) {
                if (baseUrlRef.getId() == newEndpoint.getId()) {
                    newEndpointId = String.valueOf(newEndpoint.getId());
                }
            }

            checkIfEqual(String.valueOf(baseUrlRef.getId()), newEndpointId, commentList, "id");

            EndpointType endpointResponse = new EndpointType();
            endpointResponse.setName(newEndpointId);

            String comment = StringUtils.join(commentList, ",");
            endpointResponse.setComment(comment);
            endpointResponse.setValid(StringUtils.isBlank(comment));

            result.getEndpoints().add(endpointResponse);
        }
    }

    void validateGroups(Groups groups, List<com.rackspace.idm.domain.entity.Group> newGroups, UserType user) {
        List<String> commentList;

        for (Group group : groups.getGroup()) {
            commentList = new ArrayList<String>();

            String newGroupName = null;
            String newGroupId = null;

            for (com.rackspace.idm.domain.entity.Group newGroup : newGroups) {
                if (group.getName().equals(newGroup.getName())) {
                    newGroupName = newGroup.getName();
                    newGroupId = String.valueOf(newGroup.getGroupId());
                    break;
                }
            }

            checkIfEqual(group.getName(), newGroupName, commentList, "name");
            checkIfEqual(group.getId(), newGroupId, commentList, "id");

            GroupType groupResponse = new GroupType();
            groupResponse.setName(newGroupName);
            groupResponse.setId(newGroupId);

            String comment = StringUtils.join(commentList, ",");
            groupResponse.setComment(comment);
            groupResponse.setValid(StringUtils.isBlank(comment));

            user.getGroups().add(groupResponse);
        }
    }

    void validateRoles(List<TenantRole> roles, RoleList newRoles, UserType user) {
        List<String> commentList;

        for (Role role : newRoles.getRole()) {
            commentList = new ArrayList<String>();
            String newRoleName = null;
            String newRoleId = null;
            for (TenantRole newRole : roles) {
                if (role.getName().equals(newRole.getName())) {
                    newRoleName = newRole.getName();
                    newRoleId = newRole.getRoleRsId();
                    break;
                }
            }

            checkIfEqual(role.getName(), newRoleName, commentList, "name");
            checkIfEqual(role.getId(), newRoleId, commentList, "id");

            RoleType roleResponse = new RoleType();
            roleResponse.setName(newRoleName);
            roleResponse.setId(newRoleId);
            String comment = StringUtils.join(commentList, ",");
            roleResponse.setComment(comment);
            roleResponse.setValid(StringUtils.isBlank(comment));

            user.getRoles().add(roleResponse);
        }
    }

    private void checkIfEqual(String oldValue, String newValue, List<String> commentList, String id) {
        checkIfEqual(oldValue, newValue, commentList, id, false);
    }

    private void checkIfEqual(String oldValue, String newValue, List<String> commentList, String id, boolean mask) {
        String defaultOldValue = StringUtils.defaultString(oldValue);
        String defaultNewValue = StringUtils.defaultString(newValue);

        if (!StringUtils.equals(defaultOldValue, defaultNewValue)) {
            if (mask)
                commentList.add(id + ":*****");
            else
                commentList.add(id + ":" + defaultOldValue);
        }
    }

    String getApiKey(CredentialListType credentialListType) {
        String apiKey = "";

        try {
            List<JAXBElement<? extends CredentialType>> creds = credentialListType.getCredential();
            for (JAXBElement<? extends CredentialType> cred : creds) {
                if (cred.getValue() instanceof ApiKeyCredentials) {
                    ApiKeyCredentials apiKeyCredentials = (ApiKeyCredentials) cred.getValue();
                    apiKey = apiKeyCredentials.getApiKey();
                }
            }
        } catch (Exception cex) {   // catches null pointer exceptions.

        }
        return apiKey;
    }

    String getPassword(CredentialListType credentialListType) {
        String password = "";

        try {
            List<JAXBElement<? extends CredentialType>> creds = credentialListType.getCredential();
            for (JAXBElement<? extends CredentialType> cred : creds) {
                if (cred.getValue() instanceof PasswordCredentialsRequiredUsername) {
                    PasswordCredentialsRequiredUsername passwordCredentials = (PasswordCredentialsRequiredUsername) cred.getValue();
                    password = passwordCredentials.getPassword();
                }
            }
        } catch (Exception cex) {  // catches null pointer exceptions.

        }
        return password;
    }

    AuthenticateResponse authenticate(String username, String apiKey,
                                              String password) throws Exception {
        client.setCloud20Host(config.getString("cloudAuth20url"));
        client.setCloud11Host(config.getString("cloudAuth11url"));
        AuthenticateResponse authenticateResponse;
        try {
            if (!StringUtils.isEmpty(apiKey))
                authenticateResponse = client.authenticateWithApiKey(username, apiKey);
            else if (!StringUtils.isEmpty(password))
                authenticateResponse = client.authenticateWithPassword(username, password);
            else {
                throw new BadRequestException("Failed migration with incomplete credential information.");
            }
        }
        catch (Exception ex) {
            throw new BadRequestException("Failed migration with incomplete credential information.");
        }
        return authenticateResponse;
    }

    boolean isUserAdmin(RoleList roles) {
        for (Role role : roles.getRole()) {
            if ("identity:user-admin".equalsIgnoreCase(role.getName())) {
                return true;
            }
        }
        return false;
    }

    boolean isSubUser(RoleList roles) {
        for (Role role : roles.getRole()) {
            if ("identity:default".equalsIgnoreCase(role.getName())) {
                return true;
            }
        }
        return false;
    }

    public void setMigratedUserEnabledStatus(String username, boolean enable) {
        com.rackspace.idm.domain.entity.User user = userService.getUser(username);
        if (user == null)
            throw new NotFoundException("User not found.");
        if (user.getInMigration() == null)
            throw new NotFoundException("User not found.");
        user.setInMigration(enable);
        userService.updateUserById(user, false);
    }

    public void unmigrateUserByUsername(String username) throws Exception {
        com.rackspace.idm.domain.entity.User user = userService.getUser(username);
        if (user == null)
            throw new NotFoundException("User not found.");
        if (user.getInMigration() == null) // Used so we do not delete a user who wasn't previously migrated.
            throw new NotFoundException("User not found.");

        String domainId = user.getDomainId();
        FilterParam[] filters = new FilterParam[]{new FilterParam(FilterParam.FilterParamName.DOMAIN_ID, domainId)};
        Users users = this.userService.getAllUsers(filters, 0, 0);

        if (users.getUsers() == null) // Used so we do not delete a user who wasn't previously migrated.
            throw new NotFoundException("User not found.");

        for (com.rackspace.idm.domain.entity.User u : users.getUsers()) {
            if (u.getInMigration() == null) {
                throw new ConflictException("Cannot unmigrate useradmin that contains subusers created after migration.");
            }
        }
        
        for (com.rackspace.idm.domain.entity.User u : users.getUsers())
            userService.deleteUser(u.getUsername());
    }

    String getAdminToken() throws URISyntaxException, HttpException, IOException, JAXBException {
        client.setCloud20Host(config.getString("cloudAuth20url"));
        client.setCloud11Host(config.getString("cloudAuth11url"));
        try {
            String adminUsername = config.getString("migration.username");
            String adminApiKey = config.getString("migration.apikey");
            AuthenticateResponse authenticateResponse = client.authenticateWithApiKey(adminUsername, adminApiKey);
            return authenticateResponse.getToken().getId();
        } catch (Exception ex) {
            throw new NotAuthenticatedException("Admin credentials are invalid");
        }
    }

    com.rackspace.idm.domain.entity.User addMigrationUser(User user,
                                                                  int mossoId,
                                                                  String nastId,
                                                                  String apiKey,
                                                                  String password,
                                                                  SecretQA secretQA,
                                                                  String domainId) {
        com.rackspace.idm.domain.entity.User newUser = new com.rackspace.idm.domain.entity.User();
        newUser.setId(user.getId());
        newUser.setUsername(user.getUsername());

        newUser.setMossoId(mossoId);
        newUser.setNastId(nastId);

        if (!StringUtils.isEmpty(user.getEmail()))
            newUser.setEmail(user.getEmail());

        newUser.setApiKey(apiKey);
        newUser.setPassword(password);
        newUser.setEnabled(user.isEnabled());

        if(user.getCreated() != null)
            newUser.setCreated(new DateTime(user.getCreated().toGregorianCalendar().getTime()));

        if (user.getUpdated() != null)
            newUser.setUpdated(new DateTime(user.getUpdated().toGregorianCalendar().getTime()));

        if (secretQA != null) {
            newUser.setSecretQuestion(secretQA.getQuestion());
            newUser.setSecretAnswer(secretQA.getAnswer());
        }

        newUser.setInMigration(true);
        newUser.setMigrationDate(new DateTime());

        if (domainId != null) {
            newUser.setDomainId(domainId);
        }

        String defaultRegion = getDefaultRegion(user);
        if (defaultRegion != null) {
            newUser.setRegion(defaultRegion);
        }

        userService.addUser(newUser);
        return newUser;
    }

    void addUserGroups(String userId, Groups groups) throws Exception {
        try {
            for (Group group : groups.getGroup()) {
                if (isUkCloudRegion() && group.getId().equals("1"))  {// Set Default (1) to Default (0)
                    group.setId("0");
                }
                cloudGroupService.addGroupToUser(Integer.parseInt(group.getId()), userId);
            }
        } catch (Exception ex) {
        
        }
    }

    void addUserGlobalRoles(com.rackspace.idm.domain.entity.User user, RoleList roleList) {

        List<ClientRole> clientRoles = applicationService.getAllClientRoles(null);
        for (Role role : roleList.getRole()) {
            for (ClientRole cRole : clientRoles) {
                if (StringUtils.equals(cRole.getName(), role.getName())) {
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

    void addTenantsForUserByToken(com.rackspace.idm.domain.entity.User user, String tenantId, List<String> baseUrlRefs) throws Exception {
        if (baseUrlRefs != null) {
            com.rackspace.idm.domain.entity.Tenant newTenant = tenantService.getTenant(tenantId);
            // Add the Tenant if it doesn't exist.
            if (newTenant == null) {
                // Add new Tenant
                addTenant(tenantId, baseUrlRefs.toArray(new String[0]));
            }
            // Add roles to user on tenant

            for (String baseUrl : baseUrlRefs) {
                addTenantRole(user, tenantId, Integer.parseInt(baseUrl));
            }
        }
    }

    void addTenant(String tenantId, String[] baseUrls) {
        com.rackspace.idm.domain.entity.Tenant newTenant = new com.rackspace.idm.domain.entity.Tenant();
        newTenant.setTenantId(tenantId);
        newTenant.setName(tenantId);
        newTenant.setEnabled(true);
        newTenant.setBaseUrlIds(baseUrls);
        tenantService.addTenant(newTenant);
    }

    void addTenantRole(com.rackspace.idm.domain.entity.User user, String tenantId, int endpointId) {
        CloudBaseUrl cloudBaseUrl = endpointService.getBaseUrlById(endpointId);
        Application application = applicationService.getByName(cloudBaseUrl.getServiceName());
        if (application == null) {
            logger.debug("Unknown application detected");
            // ToDo: Should we throw an error and roll everything back?
        } else {
            List<ClientRole> clientRoles = applicationService.getClientRolesByClientId(application.getClientId());

            for (ClientRole role : clientRoles) {
                TenantRole tenantRole = new TenantRole();
                tenantRole.setClientId(application.getClientId());
                tenantRole.setUserId(user.getId());
                tenantRole.setTenantIds(new String[]{tenantId}); //ToDo: does this overwrite a previous?
                tenantRole.setName(role.getName());
                tenantRole.setRoleRsId(role.getId());
                tenantService.addTenantRoleToUser(user, tenantRole);
            }
        }
    }

    private SecretQA getSecretQA(String adminToken, String userId) throws Exception {
        client.setCloud20Host(config.getString("cloudAuth20url"));
        client.setCloud11Host(config.getString("cloudAuth11url"));
        try {
            SecretQA secretQA = client.getSecretQA(adminToken, userId);
            return secretQA;
        }
        catch (Exception ex) {
            return null;
        }
    }

    void addOrUpdateGroups(String adminToken) throws Exception {
        client.setCloud20Host(config.getString("cloudAuth20url"));
        client.setCloud11Host(config.getString("cloudAuth11url"));
        Groups groups = client.getGroups(adminToken);
        if (groups != null) {
            for (Group group : groups.getGroup()) {
                com.rackspace.idm.domain.entity.Group newGroup = new com.rackspace.idm.domain.entity.Group();
                newGroup.setGroupId(Integer.parseInt(group.getId()));
                newGroup.setName(group.getName());
                if (StringUtils.isBlank(group.getDescription())) {
                    newGroup.setDescription(group.getName());
                } else {
                    newGroup.setDescription(group.getDescription());
                }

                try {
                    com.rackspace.idm.domain.entity.Group oldGroup = cloudGroupService.getGroupById(Integer.parseInt(group.getId()));
                    if (!StringUtils.equals(newGroup.getName(), oldGroup.getName()) || !StringUtils.equals(newGroup.getDescription(), oldGroup.getDescription()))
                        cloudGroupService.updateGroup(newGroup);
                }
                catch (NotFoundException ex) {
                    cloudGroupService.insertGroup(newGroup);
                }
            }
        }
    }

    void addOrUpdateEndpointTemplates(String adminToken) throws Exception {
        client.setCloud20Host(config.getString("cloudAuth20url"));
        client.setCloud11Host(config.getString("cloudAuth11url"));
        // Using Endpoints call to get Keystone Endpoint
        EndpointTemplateList endpoints = client.getEndpointTemplates(adminToken);

        //Get V1.1 BaseURLs for extra info
        BaseURLList baseURLs;
        try {
            baseURLs = client.getBaseUrls(config.getString("ga.username"), config.getString("ga.password"));
        }
        catch (Exception ex) {
            baseURLs = new BaseURLList();
        }
        if (endpoints != null) {
            for (EndpointTemplate endpoint : endpoints.getEndpointTemplate()) {
                BaseURL baseURL = getBaseUrlFromEndpoint(endpoint.getId(), baseURLs.getBaseURL());
                if (isUkCloudRegion()) {
                    endpoint.setId(endpoint.getId() + UK_BASEURL_OFFSET);
                }

                CloudBaseUrl cloudBaseUrl = endpointService.getBaseUrlById(endpoint.getId());

                if (cloudBaseUrl == null) {
                    cloudBaseUrl = copyCloudBaseUrlFromEndpointTemplate(endpoint);

                    //Use V1.1 to add additional info
                    if (baseURL != null) {
                        cloudBaseUrl.setBaseUrlType(baseURL.getUserType().value());
                        cloudBaseUrl.setDef(baseURL.isDefault());
                    } else {
                        cloudBaseUrl.setBaseUrlType("MOSSO");
                        cloudBaseUrl.setDef(false);
                    }
                    endpointService.addBaseUrl(cloudBaseUrl);
                } else {
                    String uid = cloudBaseUrl.getUniqueId();
                    cloudBaseUrl = copyCloudBaseUrlFromEndpointTemplate(endpoint);
                    cloudBaseUrl.setUniqueId(uid);

                    //Use V1.1 to add additional info
                    if (baseURL != null) {
                        cloudBaseUrl.setBaseUrlType(baseURL.getUserType().value());
                        cloudBaseUrl.setDef(baseURL.isDefault());
                    } else {
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

        if (!StringUtils.isBlank(endpoint.getPublicURL()))
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

    BaseURL getBaseUrlFromEndpoint(int endpointId, List<BaseURL> baseURLs) {
        for (BaseURL b : baseURLs) {
            if (b.getId() == endpointId)
                return b;
        }
        return null;
    }

    private boolean isUkCloudRegion() {
        if ("UK".equalsIgnoreCase(config.getString("cloud.region"))) {
            return true;
        } else {
            return false;
        }
    }

    public void setClient(MigrationClient client) {
        this.client = client;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setTenantService(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    public void setApplicationService(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    public void setEndpointService(EndpointService endpointService) {
        this.endpointService = endpointService;
    }

    public void setCloudGroupService(GroupService cloudGroupService) {
        this.cloudGroupService = cloudGroupService;
    }

    public void setScopeAccessService(ScopeAccessService scopeAccessService) {
        this.scopeAccessService = scopeAccessService;
    }

    public void setConfig(Configuration config) {
        this.config = config;
    }

    public void setObj_factories(JAXBObjectFactories obj_factories) {
        this.obj_factories = obj_factories;
    }

    public void setUserConverterCloudV20(UserConverterCloudV20 userConverterCloudV20) {
        this.userConverterCloudV20 = userConverterCloudV20;
    }

    public void setRoleConverterCloudV20(RoleConverterCloudV20 roleConverterCloudV20) {
        this.roleConverterCloudV20 = roleConverterCloudV20;
    }

    public void setEndpointConverterCloudV20(EndpointConverterCloudV20 endpointConverterCloudV20) {
        this.endpointConverterCloudV20 = endpointConverterCloudV20;
    }

    public void setCloudKsGroupBuilder(CloudKsGroupBuilder cloudKsGroupBuilder) {
        this.cloudKsGroupBuilder = cloudKsGroupBuilder;
    }

    public void setAtomHopperClient(AtomHopperClient atomHopperClient) {
        this.atomHopperClient = atomHopperClient;
    }
}
