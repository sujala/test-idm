package com.rackspace.idm.domain.service.impl;

import com.google.common.collect.Iterables;
import com.rackspace.docs.event.identity.user.credential.CredentialTypeEnum;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.*;
import com.rackspace.idm.ErrorCodes;
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperClient;
import com.rackspace.idm.api.resource.cloud.atomHopper.FeedsUserStatusEnum;
import com.rackspace.idm.api.resource.cloud.atomHopper.CredentialChangeEventData;
import com.rackspace.idm.api.resource.cloud.v20.PaginationParams;
import com.rackspace.idm.api.security.AuthenticationContext;
import com.rackspace.idm.api.security.IdentityRole;
import com.rackspace.idm.api.security.ImmutableTenantRole;
import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.*;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.entity.DelegationAgreement;
import com.rackspace.idm.domain.entity.Domain;
import com.rackspace.idm.domain.entity.Region;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.*;
import com.rackspace.idm.modules.usergroups.Constants;
import com.rackspace.idm.modules.usergroups.entity.UserGroup;
import com.rackspace.idm.multifactor.service.MultiFactorService;
import com.rackspace.idm.util.CryptHelper;
import com.rackspace.idm.util.HashHelper;
import com.rackspace.idm.util.IdmCommonUtils;
import com.rackspace.idm.validation.Validator;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.time.Duration;
import java.util.*;

import static com.rackspace.idm.GlobalConstants.*;

@Component
public class DefaultUserService implements UserService {
    public static final String GETTING_USER = "Getting User: {}";
    public static final String GOT_USER = "Got User: {}";
    static final String ENCRYPTION_VERSION_ID = "encryptionVersionId";
    private static final String DELETE_USER_FORMAT = "DELETED username={},domainId={},roles={}";

    private static final String ERROR_MSG_SAVE_OR_UPDATE_USER = "Error updating user %s";
    private static final String ERROR_MSG_TENANT_ALREADY_EXISTS = "Tenant with Id '%s' already exists";
    private static final String ERROR_MSG_TENANT_DOES_NOT_EXIST = "Tenant with Id '%s' does not exist";
    private static final String ERROR_MSG_NEW_ACCOUNT_IN_DEFAULT_DOMAIN = "Can not create a new account in the default domain";
    private static final String ERROR_MSG_NEW_ACCOUNT_IN_DISABLED_DOMAIN = "Can not create a new account in a disabled domain";
    private static final String ERROR_MSG_NEW_ACCOUNT_IN_DOMAIN_WITH_USERS = "Can not create a new account in an existing domain with users";
    private static final String ERROR_MSG_NEW_ACCOUNT_EXISTING_TENANT_DIFFERENT_DOMAIN = "Can not create new account with an an existing tenant in a different domain";
    private static final String ERROR_MSG_NEW_ACCOUNT_EXISTING_TENANT_WITH_USERS = "Can not create new account with an existing tenant that has users";

    public static final String ERROR_PATTERN_MAX_NUM_USERS_IN_DOMAIN = "User cannot create more than %d users in an account.";

    public static final String ERROR_MSG_TOKEN_NOT_FOUND = "Token not found.";

    static final String MOSSO_BASE_URL_TYPE = "MOSSO";
    static final String NAST_BASE_URL_TYPE = "NAST";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Logger deleteUserLogger = LoggerFactory.getLogger(DELETE_USER_LOG_NAME);

    @Autowired
    private ScopeAccessService scopeAccessService;

    @Autowired
    private RackerAuthDao rackerAuthDao;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private Configuration config;

    @Autowired
    private IdentityConfig identityConfig;

    @Autowired
    private UserDao userDao;

    @Autowired
    private FederatedUserDao federatedUserDao;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private EndpointService endpointService;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private CloudRegionService cloudRegionService;

    @Autowired
    private Validator validator;

    @Autowired
    private DomainService domainService;

    @Autowired
    private PropertiesService propertiesService;

    @Autowired
    private CryptHelper cryptHelper;

    @Autowired
    private RoleService roleService;

    @Autowired
    private MultiFactorService multiFactorService;

    @Autowired
    private IdentityUserService identityUserService;

    @Autowired
    private ProvisionedUserFederationHandler federationHandler;

    @Autowired
    private AuthenticationContext authenticationContext;

    @Autowired
    private CreateSubUserService createSubUserService;

    @Autowired
    private DelegationService delegationService;

    @Autowired
    private TenantRoleDao tenantRoleDao;

    @Autowired
    private TenantAssignmentService tenantAssignmentService;

    @Autowired
    private AtomHopperClient atomHopperClient;

    @Autowired
    private PhonePinService phonePinService;

    @Autowired
    @Qualifier("tokenRevocationService")
    private TokenRevocationService tokenRevocationService;

    @Autowired
    private IdmCommonUtils idmCommonUtils;

    @Override
    public void addIdentityAdminV20(User user) {
        addUserV20(user, false, false, false);
    }

    @Override
    public void addSubUserV20(User user, boolean isCreateUserOneCall) {
        //creating a sub-user so always a one-user call but do not provision the mosso and nast
        addUserV20(user, isCreateUserOneCall, false, false);
    }

    @Override
    public void addUserAdminV20(User user, boolean isCreateUserOneCall) {
        //only provision mosso and nast tenants if this is a one-user call and the domain is numeric
        addUserV20(user, isCreateUserOneCall,  isCreateUserOneCall && idmCommonUtils.isNumeric(user.getDomainId()), true);
    }

    private void addUserV20(User user, boolean isCreateUserInOneCall, boolean provisionMossoAndNast, boolean isUserAdmin) {

        logger.info("Adding User: {}", user);

        validator.validateUser(user);

        verifyCallerCanCreateTenants(user, provisionMossoAndNast);

        Domain domain;

        if (isUserAdmin || isCreateUserInOneCall) {
            domain = createDomainUserInCreateOneCall(user.getDomainId(), isUserAdmin);
        } else {
            // NOTE: This allows creating an identity:admin user in the default domain.
            domain = createDomainIfItDoesNotExist(user.getDomainId());
        }

        if (isCreateUserInOneCall) {
            verifyUserRolesExist(user);
            if(userContainsRole(user, IdentityUserTypeEnum.USER_ADMIN.getRoleName())) {
                verifyUserTenantsInCreateOneCall(user);
            } else if(userContainsRole(user, IdentityUserTypeEnum.DEFAULT_USER.getRoleName())) {
                verifyUserTenantsExist(user);
            } else {
                //if we get here, then the user being created is a service or identity admin
                //this should not happen but checking anyways
                throw new BadRequestException();
            }

            if(provisionMossoAndNast) {
                createDefaultDomainTenantsInCreateOneCall(domain);
            }
            createTenantsIfNecessary(user);
        }
        checkMaxNumberOfUsersInDomain(user.getDomainId());

        setPasswordIfNotProvided(user);
        setApiKeyIfNotProvided(user);
        setRegionIfNotProvided(user, domain);

        // Generate phone pin if not provided
        if (StringUtils.isBlank(user.getPhonePin())) {
            user.updatePhonePin(phonePinService.generatePhonePin());
        }

        if (provisionMossoAndNast) {
            //hack alert!! code requires the user object to have the nastid attribute set. this attribute
            //should no longer be required as users have roles on a tenant instead. once this happens, remove
            user.setNastId(getNastTenantId(user.getDomainId()));
            user.setMossoId(Integer.parseInt(user.getDomainId()));
        }

        user.setEncryptionVersion(propertiesService.getValue(ENCRYPTION_VERSION_ID));
        user.setSalt(cryptHelper.generateSalt());
        user.setEnabled(user.getEnabled() == null ? true : user.getEnabled());

        userDao.addUser(user);

        atomHopperClient.asyncPost(user, FeedsUserStatusEnum.CREATE, MDC.get(Audit.GUUID));

        assignUserRoles(user, isCreateUserInOneCall);

        if (isUserAdmin) {
            // Update domain's userAdminDN after the user has been created.
            domainService.updateDomainUserAdminDN(user);
        }
    }

    @Override
    public void addUnverifiedUser(User unverifiedUser) {
        logger.info("Adding User: {}", unverifiedUser);
        Validate.isTrue(StringUtils.isNotBlank(unverifiedUser.getDomainId()), "Unverified users must have a domain ID.");
        Validate.isTrue(StringUtils.isNotBlank(unverifiedUser.getEmail()), "Unverified users must have an email.");

        DomainSubUserDefaults defaults;
        try {
            defaults = createSubUserService.calculateDomainSubUserDefaults(unverifiedUser.getDomainId());
        } catch (DomainDefaultException ex) {
            throw new ForbiddenException(ErrorCodes.ERROR_CODE_UNVERIFIED_USERS_DOMAIN_WITHOUT_ACCOUNT_ADMIN_MESSAGE, ErrorCodes.ERROR_CODE_UNVERIFIED_USERS_DOMAIN_WITHOUT_ACCOUNT_ADMIN);
        }
        unverifiedUser.setRegion(defaults.getRegion());
        unverifiedUser.setRsGroupId(defaults.getRateLimitingGroupIds());
        Collection<TenantRole> tenantRoles =  CollectionUtils.collect(defaults.getSubUserTenantRoles(), new Transformer<ImmutableTenantRole, TenantRole>() {
            @Override
            public TenantRole transform(ImmutableTenantRole input) {
                return input.asTenantRole();
            }
        });
        unverifiedUser.setRoles(new ArrayList<TenantRole>(tenantRoles));
        CreateUserUtil.attachRoleToUser(roleService.getDefaultRole(), unverifiedUser);

        checkMaxNumberOfUsersInDomain(unverifiedUser.getDomainId());

        unverifiedUser.setNastId(getNastTenantId(unverifiedUser.getDomainId()));
        unverifiedUser.setMossoId(Integer.parseInt(unverifiedUser.getDomainId()));
        unverifiedUser.setEncryptionVersion(propertiesService.getValue(ENCRYPTION_VERSION_ID));
        unverifiedUser.setSalt(cryptHelper.generateSalt());
        unverifiedUser.setEnabled(false);
        unverifiedUser.setUnverified(true);

        userDao.addUser(unverifiedUser);

        assignUserRoles(unverifiedUser, false);
    }

    private boolean userContainsRole(User user, String roleName) {
        for (TenantRole role : user.getRoles()) {
            if(roleName.equalsIgnoreCase(role.getName())) {
                return true;
            }
        }
        return false;
    }

    private void verifyUserRolesExist(User user) {
        for (TenantRole role : user.getRoles()) {
            ClientRole roleObj = roleService.getRoleByName(role.getName());
            if (roleObj == null) {
                throw new BadRequestException(String.format("Role '%s' does not exist.", role.getName()));
            }
        }

    }

    private void verifyUserTenantsDoNotExist(User user) {
        for (TenantRole role : user.getRoles()) {
            for (String tenantId : role.getTenantIds()) {
                if (tenantService.getTenant(tenantId) != null) {
                    throw new BadRequestException(String.format(ERROR_MSG_TENANT_ALREADY_EXISTS, tenantId));
                }
            }
        }
    }

    private void verifyUserTenantsExist(User user) {
        for (TenantRole role : user.getRoles()) {
            for (String tenantId : role.getTenantIds()) {
                if (tenantService.getTenant(tenantId) == null) {
                    throw new BadRequestException(String.format(ERROR_MSG_TENANT_DOES_NOT_EXIST, tenantId));
                }
            }
        }
    }

    private void createTenantsIfNecessary(User user) {
        Domain domain = domainService.getDomain(user.getDomainId());
        String[] tenantIds = tenantService.getTenantIdsForDomain(domain);
        List<String> domainTenants = tenantIds != null ? new ArrayList<String>(Arrays.asList(tenantIds)) : new ArrayList<String>();

        for (TenantRole role : user.getRoles()) {
            for (String tenantId : role.getTenantIds()) {
                Tenant tenant = new Tenant();
                tenant.setName(tenantId);
                tenant.setTenantId(tenantId);
                tenant.setDisplayName(tenantId);
                tenant.setEnabled(true);
                if (identityConfig.getReloadableConfig().shouldSetDefaultTenantTypeOnCreation()) {
                    String tenantType = tenantService.inferTenantTypeForTenantId(tenantId);
                    if (StringUtils.isNotBlank(tenantType)) {
                        tenant.getTypes().add(tenantType);
                    }
                }

                try {
                    tenantService.addTenant(tenant);
                } catch (DuplicateException dex) {
                    // no-op
                    // We've already checked to make sure the tenants did not exist prior to this call
                    // so a duplicate would only happen when we try to add the mosso and nast tenant
                    // again or if a user was given more than one role on a tenant. In either of these
                    // cases we don't want to error out, we just want to catch and move on.
                }

                if (!domainTenants.contains(tenantId)) {
                    domainService.addTenantToDomain(tenantId, user.getDomainId());
                    domainTenants.add(tenantId);
                }
            }
        }

    }

    @Override
    public UserAuthenticationResult authenticate(String username, String password) {
        logger.debug("Authenticating User: {} by Username", username);
        UserAuthenticationResult authenticated = userDao.authenticate(username, password);

        if (authenticated.isAuthenticated()) {
            Domain domain = null;
            User user = (User) authenticated.getUser();

            // TODO: Would throw an NPE if user is not set in auth result.
            if (StringUtils.isNotEmpty(user.getDomainId())) {
                domain = domainService.getDomain(user.getDomainId());

                // Set the domain within the authentication context if auth succeeded
                authenticationContext.setDomain(domain);
                if (domain == null) {
                    logger.error("User with ID {} references domain with ID {} but the domain does not exist.",
                            user.getId(), user.getDomainId());
                }
            }

            // Apply password rotation policy if applicable
            if (domain != null ) {
                DateTime pwdExpireDate = getPasswordExpiration(user, domain);
                boolean expired = isPasswordExpired(user, domain);
                if (pwdExpireDate != null) {
                    authenticationContext.setPasswordExpiration(pwdExpireDate);
                }
                if (expired) {
                    throw new UserPasswordExpiredException(user, "The password for this user has expired and must be changed");
                }
            }
        }

        /*
        If the user within authresult is not null and authentication succeeded, verifies it and associated domain are enabled.
        If either disabled, throws UserDisabledException("User '" + user.getUsername() +"' is disabled.");
         */
        validateUserStatus(authenticated);
        logger.debug("Authenticated User: {} by Username - {}", username,
                authenticated);
        return authenticated;
    }

    @Override
    public DateTime getPasswordExpiration(User user) {
        Validate.notNull(user);
        Domain domain = domainService.getDomain(user.getDomainId());

        if (domain != null) {
            return getPasswordExpiration(user, domain);
        } else {
            return null;
        }
    }

    /**
     * Calculates the user's password expiration based on the password policy of the domain provided.
     * Returns null if:
     *  - the user has a null password change date
     *  - the domain does not have a password policy duration set
     *
     * @param user
     * @param domain
     * @throws IllegalArgumentException if the domain or user is null
     * @return
     */
    private DateTime getPasswordExpiration(User user, Domain domain) {
        Validate.notNull(user);
        Validate.notNull(domain);
        Date pwdChangeDate = user.getPasswordLastUpdated();
        Duration duration = domain.getPasswordPolicy() != null ? domain.getPasswordPolicy().getPasswordDurationAsDuration() : null;

        if (pwdChangeDate != null && duration != null && !duration.isZero()) {
            long durationMs = duration.toMillis();
            return new DateTime(pwdChangeDate).plus(durationMs);
        }

        return null;
    }

    @Override
    public boolean isPasswordExpired(User user) {
        Validate.notNull(user);
        Domain domain = domainService.getDomain(user.getDomainId());

        if (domain != null) {
            return isPasswordExpired(user, domain);
        } else {
            return false;
        }
    }

    /**
     * Determines if the user's password is expired based on the password policy of the provided domain.
     *
     * @param user
     * @throws IllegalArgumentException if the domain or user is null
     * @return
     */
    public boolean isPasswordExpired(User user, Domain domain) {
        Validate.notNull(user);
        Validate.notNull(domain);
        PasswordPolicy passwordPolicy = domain.getPasswordPolicy();
        Duration domainPwdDuration = passwordPolicy != null ? passwordPolicy.getPasswordDurationAsDuration() : null;

        if (domainPwdDuration != null && !domainPwdDuration.isZero()) {
            Date userPwdChangeDate = user.getPasswordLastUpdated();
            if (userPwdChangeDate == null) {
                // All users w/o a pwd change date and a non-zero domain pwd duration have expired passwords
                return true;
            } else {
                // Otherwise, the user's password is considered expired if it is older than the allowed domain pwd policy duration
                DateTime pwdExpiration = new DateTime(userPwdChangeDate).plus(domainPwdDuration.toMillis());
                return new DateTime().isAfter(pwdExpiration);
            }
        }

        return false;
    }

    @Override
    public UserAuthenticationResult authenticateWithApiKey(String username,
                                                           String apiKey) {
        logger.debug("Authenticating User: {} by API Key", username);
        UserAuthenticationResult authenticated = userDao.authenticateByAPIKey(username, apiKey);
        validateUserStatus(authenticated);
        logger.debug("Authenticated User: {} by API Key - {}", username,
                authenticated);
        return authenticated;
    }

    @Override
    public User loadUser(String userId) {
        User user = this.getUserById(userId);
        if (user == null) {
            String errorMsg = String.format("User not found with id: %s", userId);
            logger.warn(errorMsg);
            throw new NotFoundException(errorMsg);
        }
        return user;
    }

    @Override
    public void deleteUser(User user) {
        logger.info("Deleting User: {}", user.getUsername());

        List<TenantRole> roles = this.tenantService.getTenantRolesForUser(user);
        if(StringUtils.isNotBlank(user.getExternalMultiFactorUserId())) {
            multiFactorService.removeMultiFactorForUser(user.getId());
        }
        delegationService.removeConsumerFromExplicitDelegationAgreementAssignments(user);
        this.userDao.deleteUser(user);
        deleteUserLogger.warn(DELETE_USER_FORMAT,
                new Object[] {user.getUsername(), user.getDomainId(), roles.toString()});

        logger.info("Deleted User: {}", user.getUsername());
    }

    @Override
    public void deleteUser(String username) {
        User user = this.userDao.getUserByUsername(username);
        this.deleteUser(user);
    }

    @Override
    public String generateApiKey() {
            return HashHelper.getRandomSha1();
    }

    @Override
    public Iterable<User> getUsersByUsername(String username) {
        logger.debug("Getting All Users");

        return this.userDao.getUsersByUsername(username);
    }

    @Override
    public List<User> getAllUsers() {
        //TODO: Pagination
        logger.debug("Getting All Users");

        PaginatorContext<User> users = this.userDao.getUsers(getLdapPagingOffsetDefault(), getLdapPagingLimitDefault());

        logger.debug("Got All Users {}");

        return users.getValueList();
    }

    @Override
    public Iterable<User> getUsersWithDomainAndEnabledFlag(String domainId, Boolean enabled) {
        logger.debug("Getting All Users: {} - {}", domainId, enabled);

        return this.userDao.getUsersByDomainAndEnabledFlag(domainId, enabled);
    }

    @Override
    public Iterable<User> getEnabledUsersByGroupId(String groupId) {
        logger.debug("Getting All Enabled Users: {} - {}", groupId);

        return this.userDao.getEnabledUsersByGroupId(groupId);
    }

    @Override
    public Iterable<User> getDisabledUsersByGroupId(String groupId) {
        logger.debug("Getting All Disabled Users: {} - {}", groupId);

        return this.userDao.getDisabledUsersByGroupId(groupId);
    }

    @Override
    public Racker getRackerByRackerId(String rackerId) {
        Racker racker = new Racker();

        String username = rackerId;
        if (Racker.isFederatedRackerId(rackerId)) {
            username = Racker.getUsernameFromFederatedId(rackerId);

        }
        racker.setRackerId(rackerId);
        racker.setUsername(username);
        racker.setEnabled(true);

        return racker;
    }

    /**
     *
     * @param rackerId
     * @return
     *
     * @throws NotFoundException If racker not found in IAM
     */
    @Override
    public List<String> getRackerIamRoles(String rackerId) {
        logger.debug("Getting Roles for Racker: {}", rackerId);

        if (!isRackerAuthAllowed()) {
            throw new ForbiddenException();
        }
        String rackerIdForSearch = rackerId;

        /*
        If the id is a federated id format, must extract username from id
         */
        if (Racker.isFederatedRackerId(rackerId)) {
            rackerIdForSearch = Racker.getUsernameFromFederatedId(rackerId);
        }

        List<String> roles;
        if (identityConfig.getReloadableConfig().cacheRackerGroups()) {
            roles = rackerAuthDao.getRackerRolesWithCache(rackerIdForSearch);
        } else {
            roles = rackerAuthDao.getRackerRoles(rackerIdForSearch);
        }
        logger.debug("Got Roles for Racker: {}", rackerId);
        return roles;
    }

    @Override
    public User getUser(String username) {
        logger.debug(GETTING_USER, username);
        User user = userDao.getUserByUsername(username);
        logger.debug(GOT_USER, user);
        return user;
    }

    @Override
    public Iterable<User> getUsersByEmail (String email, User.UserType userType) {
        logger.debug(GETTING_USER, email);
        return userDao.getUsersByEmail(email, userType);
    }

    @Override
    public User getUserByAuthToken(String authToken) {
        if (authToken == null) {
            return null;
        }
        ScopeAccess scopeAccessByAccessToken = scopeAccessService.getScopeAccessByAccessToken(authToken);
        if(scopeAccessByAccessToken == null) {
            return null;
        }

        User user = null;
        /*
         * existing code appears to have returned null if the specified token was not associated with a provisioned user
         * (e.g. was a federated
         * user, racker, or a non-user based token).
         *
         */
        if (scopeAccessByAccessToken instanceof BaseUserToken) {
            String userId = ((BaseUserToken)scopeAccessByAccessToken).getIssuedToUserId();
            user = identityUserService.getProvisionedUserById(userId);
        }

        return user;               
    }

    @Override
    public BaseUser getTokenSubject(String token) {
        if (token == null) {
            return null;
        }
        ScopeAccess scopeAccessByAccessToken = scopeAccessService.getScopeAccessByAccessToken(token);
        if(scopeAccessByAccessToken == null) {
            return null;
        }

        BaseUser user = getUserByScopeAccess(scopeAccessByAccessToken, false);

        return user;
    }

    /**
     * This service does not use the new auto-assign role functionality for granting all users in a domain "access" to
     * all tenants w/in the domain. This service should be updated to support, if required.
     *
     * @param tenantId
     * @return
     * @deprecated - The
     */
    @Deprecated
    @Override
    public Iterable<User> getUsersByTenantId(String tenantId) {
        logger.debug("Get list of users with tenant", tenantId);
        List<TenantRole> tenantRoles = tenantService.getTenantRolesForTenant(tenantId);

        //TODO This should be a Set or should use logic to not add same user twice
        List<String> idList = new ArrayList<String>();
        for(TenantRole t : tenantRoles){
            // Ignore user group or delegation agreement tenant roles
            String path = t.getUniqueId();
            if (!(StringUtils.endsWithIgnoreCase(path, Constants.USER_GROUP_BASE_DN)
                    || StringUtils.endsWithIgnoreCase(path, LdapRepository.DELEGATION_AGREEMENT_BASE_DN))) {
                if(t.getUserId() == null) {
                    tenantService.addUserIdToTenantRole(t);
                }
                idList.add(t.getUserId());
            }
        }
        return userDao.getUsers(idList);
    }

    @Override
    public User getUserByTenantId(String tenantId) {
        logger.debug("Getting user by tenantId: {}", tenantId);

        // TODO: Note - this does NOT use the new auto-assignment methods.
        Iterable<User> users = getUsersByTenantId(tenantId);

        User result = null;
        for (User user : users) {
            if (result == null) {
                result = user;
            }
            if (authorizationService.hasUserAdminRole(user)) {
                result = user;
                break;
            }
        }

        return result;
    }

    @Override
    public void setValidator(Validator validator) {
        this.validator = validator;
    }

    @Override
    public User getUserByUsernameForAuthentication(String username) {
        User user = null;
        try {
            user = checkAndGetUserByName(username);
        } catch (NotFoundException e) {
            String errorMessage = String.format("Unable to authenticate user with credentials provided.");
            logger.warn(errorMessage);
            throw new NotAuthenticatedException(errorMessage, e);
        }
        return user;
    }

    @Override
    public User checkAndGetUserByName(String username) {
        User user = userDao.getUserByUsername(username);

        if (user == null) {
            String errMsg = String.format("User '%s' not found.", username);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        return user;
    }

    @Override
    public boolean isUsernameUnique(String username) {
        return userDao.isUsernameUnique(username);
    }

    @Override
    public List<User> getSubUsers(User user) {
    	List<User> result = new ArrayList<User>();
    	
        if (user != null) {
            for (User subUser : userDao.getUsersByDomain(user.getDomainId())) {
                if (!subUser.getId().equalsIgnoreCase(user.getId())){
                    result.add(subUser);
                }
            }
        }

        return result;
    }

    @Override
    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public void setRackerAuthDao(RackerAuthDao rackerAuthDao) {
        this.rackerAuthDao = rackerAuthDao;
    }

    @Override
    public void setApplicationService(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @Override
    public void setConfig(Configuration config) {
        this.config = config;
    }

    @Override
    public void setCloudRegionService(CloudRegionService cloudRegionService) {
        this.cloudRegionService = cloudRegionService;
    }

    @Override
    public PaginatorContext<User> getAllEnabledUsersPaged(int offset, int limit) {
        logger.debug("Getting All Enabled Users paged");

        PaginatorContext<User> users = this.userDao.getEnabledUsers(offset, limit);

        logger.debug("Got All Enabled Users paged");

        return users;
    }

    @Override
    public PaginatorContext<User> getAllUsersPagedWithDomain(String domainId, int offset, int limit) {
        logger.debug("Getting Users in Domain {}", domainId);

        PaginatorContext<User> users = this.userDao.getUsersByDomain(domainId, offset, limit);

        logger.debug("Got Users in Domain");

        return users;
    }

    @Override
    public PaginatorContext<User> getUsersWithRole(String roleId, int offset, int limit) {
        logger.debug("Getting All Users with role {}", roleId);

        List<String> userIds = tenantService.getIdsForUsersWithTenantRole(roleId, identityConfig.getStaticConfig().getUsersByRoleLimit());

        List<User> users = new ArrayList<User>();
        for (User user : this.userDao.getUsers(userIds)) {
            users.add(user);
        }

        PaginatorContext<User> context = new PaginatorContext<User>();
        context.update(users, offset, limit);

        logger.debug("Got Users with role");

        return context;
    }

    @Override
    public PaginatorContext<User> getUsersWithDomainAndRole(String domainId, String roleId, int offset, int limit) {
        logger.debug("Getting Users in Domain {}", domainId);


        List<User> users = new ArrayList<User>();
        for (User user : this.userDao.getUsersByDomain(domainId)) {
            users.add(user);
        }

        List<User> usersWithRole = filterUsersForRole(users, roleId);

        PaginatorContext<User> context = new PaginatorContext<User>();
        context.update(usersWithRole, offset, limit);

        logger.debug("Got Users in Domain");

        return context;
    }

    @Override
    public PaginatorContext<User> getEnabledUsersByGroupId(String groupId, int offset, int limit) {

        logger.debug("Getting Users in Group {}", groupId);

        PaginatorContext<User> context = userDao.getEnabledUsersByGroupId(groupId, offset, limit);

        logger.debug("Got All Users paged");

        return context;
    }

    @Override
    public Iterable<User> getEnabledUsersByContactId(String contactId) {
        logger.debug("Getting users by contactId {}", contactId);
        Validate.notNull(contactId);

        Iterable<User> users = userDao.getEnabledUsersByContactId(contactId);

        logger.debug("Got users by contactId {}", contactId);
        return users;
    }

    @Override
    public boolean hasSubUsers(String userId) {
        User user = userDao.getUserById(userId);
        if (user == null) {
            return false;
        }
        Iterable<User> users = userDao.getUsersByDomain(user.getDomainId());
        if (!users.iterator().hasNext()) {
            return false;
        }
        for (User userInList : users) {
            if(authorizationService.hasDefaultUserRole(userInList)) {
                return true;
            }
        }
        return false;
    }


    @Override
    public Password resetUserPassword(User user) {
        Password newPassword = Password.generateRandom(false, user); // Would the user
        // ever reset his
        // own password?
        userDao.updateUser(user);
        logger.debug("Updated password for user: {}", user);

        return newPassword.toExisting();
    }

    boolean checkForPasswordUpdate(User user) {
        if(user != null && !StringUtils.isEmpty(user.getPassword())) {
            return true;
        }
        return false;
    }

    @Override
    public void updateUser(User user) throws IOException, JAXBException {
        logger.info("Updating User: {}", user);

        if(!validator.isBlank(user.getEmail())){
            validator.assertEmailValid(user.getEmail());
        }
        // Expire all User tokens if we are updating the password field
        User currentUser = null;
        boolean passwordChange = checkForPasswordUpdate(user);
        if (passwordChange) {
            //load user with password history
            currentUser = identityUserService.getProvisionedUserByIdWithPwdHis(user.getId());
        } else {
            //load user without password history
            currentUser = userDao.getUserById(user.getId());
        }
        boolean userIsBeingDisabled = checkIfUserIsBeingDisabled(currentUser, user);
        boolean userIsBeingEnabled = checkIfUserIsBeingEnabled(currentUser, user);

        /*
         some mfa properties on user should never be set via updateUser call (which is used by various front end services
         such as updateUser, addUserToDomain, etc). To prevent inadvertent updating, just null them out here regardless
         of the value so the Dao will skip them. The appropriate service in MultiFactorService should be used to
         change these values
         */
        user.setMultifactorEnabled(null);
        user.setUserMultiFactorEnforcementLevel(null);
        user.setMultiFactorType(null);

        // TODO: Remove those as soon as we remove the LDAP dependencies.
        user.setReadOnlyEntry(currentUser.getReadOnlyEntry());

        user.setUniqueId(currentUser.getUniqueId());
        user.setRsGroupId(currentUser.getRsGroupId());
        user.setUserGroupDNs(currentUser.getUserGroupDNs());
        user.setEncryptionVersion(currentUser.getEncryptionVersion());
        user.setSalt(currentUser.getSalt());

        if (passwordChange) {
            performPasswordUpdateLogic(user, currentUser);
        }

        userDao.updateUser(user);

        if(passwordChange) {
            tokenRevocationService.revokeAllTokensForEndUser(user);
            if (identityConfig.getReloadableConfig().isPostCredentialChangeFeedEventsEnabled()) {
                // Copy over the values needed to populate the credential change feed event.
                // We have to check the user provided in the method params first and if not provided pull it from the existing user.
                CredentialChangeEventData credentialChangeEventData = new CredentialChangeEventData();
                credentialChangeEventData.setUserId(StringUtils.isNotBlank(user.getId()) ? user.getId() : currentUser.getId());
                credentialChangeEventData.setUsername(StringUtils.isNotBlank(user.getUsername()) ? user.getUsername() : currentUser.getUsername());
                credentialChangeEventData.setEmail(StringUtils.isNotBlank(user.getEmail()) ? user.getEmail() : currentUser.getEmail());
                credentialChangeEventData.setDomainId(StringUtils.isNotBlank(user.getDomainId()) ? user.getDomainId() : currentUser.getDomainId());
                credentialChangeEventData.setCredentialUpdateDateTime(new DateTime());
                credentialChangeEventData.setCredentialType(CredentialTypeEnum.PASSWORD);
                credentialChangeEventData.setRequestId(MDC.get(Audit.GUUID));
                atomHopperClient.asyncPostCredentialChangeEvent(credentialChangeEventData);
            }
        }


        if (userIsBeingDisabled) {
            processDisabledUser(user.getId());
        } else if (userIsBeingEnabled) {
            processEnabledUser(user.getId());
        }

        logger.info("Updated User: {}", user);
    }

    /**
     * This is run on disabled users _after_ they are disabled within Identity. It orchestrates any post processes that
     * need to occur.
     *
     * @param userId
     */
    private void processDisabledUser(String userId) throws IOException, JAXBException {
        /*
         Pull fresh user from backend to retrieve final state of the updated user. This is a little inefficient since
         the removeExternalProfile call will also retrieve a fresh user, but need the independence between the services.
        */
        User user = userDao.getUserById(userId);

        // Revoke all outstanding tokens for the user
        tokenRevocationService.revokeAllTokensForEndUser(user);

        /*
         Remove external profile (if SMS), when disabled user has MFA enabled. This breaks encapsulation of the
         mfa service a little by acknowledging the relationship between SMS and external profiles in this part
         of code, but efficiency of not reloading users unecessarily wins out.
        */
        if(user.isMultiFactorEnabled() && user.getMultiFactorTypeAsEnum() == FactorTypeEnum.SMS) {
            multiFactorService.removeExternalProfileForUser(user.getId());
        }

        // If the user is a user-admin, must disable all sub-users
        disableUserAdminSubUsers(user);
    }

    /**
     * This is run on enabled users _after_ they are enabled within Identity. It orchestrates any post processes that
     * need to occur.
     *
     * @param userId
     */
    private void processEnabledUser(String userId) {
        /*
         Pull fresh user from backend to retrieve final state of the updated user
        */
        User user = userDao.getUserById(userId);

        /*
         Add Duo profile if the user has SMS MFA enabled. This breaks encapsulation of the
         mfa service a little by acknowledging the relationship between SMS and external profiles, but efficiency of not
         reloading users unecessarily wins out.
         */
        if(user.isMultiFactorEnabled() && user.getMultiFactorTypeAsEnum() == FactorTypeEnum.SMS) {
            /*
             Note - there is an edge case here where if for some reason the enable process fails to add the profile
             to the user, the user will be in a invalid state (SMS MFA enabled, but without a profile). This is a
             possibility even when
              */
            multiFactorService.addExternalProfileForUser(user.getId());
        }
    }

    /**
     * The user's password is being changed. Need to update the provided user, as appropriate, to account for this change,
     * as well as perform various password policy checks.
     *
     * @param user - the user object will be modified as necessary for a password update
     * @param currentUser
     */
    private void performPasswordUpdateLogic(User user, User currentUser) {
        if (!checkForPasswordUpdate(user)) {
            throw new IllegalArgumentException("User's password not changing");
        }

        // If changing password, set the history on the user to keep existing history
        user.setPasswordHistory(currentUser.getPasswordHistory());
        List<String> userPasswordHistory = currentUser.getPasswordHistory();


        // Pull history policy from user's domain
        String domainForPolicy = StringUtils.isNotEmpty(user.getDomainId()) ? user.getDomainId() : currentUser.getDomainId();

        if (StringUtils.isNotEmpty(domainForPolicy)) {
            Domain domain = domainService.getDomain(domainForPolicy);
            if (domain != null && domain.getPasswordPolicy() != null) {
                PasswordPolicy policy = domain.getPasswordPolicy();

                // History is only enforced if the application wide feature is enabled AND the user's domain uses it
                boolean historyEnforced = policy.calculateEffectivePasswordHistoryRestriction() >= 0;

                // User can't set password to existing password when password policy is not null regardless of whether
                // rotation/history enforcement is actually used
                if (cryptHelper.verifyLegacySHA(user.getPassword(), currentUser.getUserPassword())) {
                    throw new BadRequestException(String.format("Must not repeat current password"), ErrorCodes.ERROR_CODE_CURRENT_PASSWORD_MATCH);
                }

                if (historyEnforced) {
                    if (CollectionUtils.isNotEmpty(userPasswordHistory)) {
                        int pwdHistoryRestriction = domain.getPasswordPolicy().calculateEffectivePasswordHistoryRestriction();
                        if (pwdHistoryRestriction > 0) {
                            /*
                            Check the password history from the end (ignoring current password) as the entries are ordered oldest first

                            Subtract 2 from history length to determine index to array (1 due to 0 based index, 1 due to last
                            entry is "current" password, which is already checked above so no point in checking again)
                            */
                            int historyIndex = userPasswordHistory.size() - 2;
                            for (int i = 0; i < pwdHistoryRestriction && historyIndex >= 0; ++i, historyIndex--) {
                                if (cryptHelper.verifyLegacySHA(user.getPassword(), userPasswordHistory.get(historyIndex))) {
                                    throw new BadRequestException(String.format("Must not repeat current or up to '%s' previous password(s)", pwdHistoryRestriction), ErrorCodes.ERROR_CODE_PASSWORD_HISTORY_MATCH);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * The boolean in the method signature to control logic is messy but necessary for now
     * in order to share common code between create user one-call logic and upgrade user logic.
     */
    @Override
    public void configureNewUserAdmin(User user, boolean assignMossoAndNastDefaultRoles) {
        if (StringUtils.isBlank(user.getDomainId())) {
            throw new BadRequestException("User-admin cannot be created without a domain");
        }

        if (getDomainRestrictedToOneUserAdmin() && domainService.getDomainAdmins(user.getDomainId()).size() != 0) {
            throw new BadRequestException("User-admin already exists for domain");
        }

        attachRoleToUser(roleService.getUserAdminRole(), user);

        if (assignMossoAndNastDefaultRoles) {
            //HACK ALERT!!! The create user in one call logic allows for creating a user with an existing
            //domain that has a domain ID that is non-numeric. This implies that the domain cannot have a
            //mosso tenant (mosso tenant domain IDs must be numeric). Thus, we need to check to see if this
            //condition is met and not assume that we can create mosso and nast tenants.
            if(idmCommonUtils.isNumeric(user.getDomainId())) {
                //original code had this. this is in place to help ensure the user has access to their
                //default tenants. currently the user-admin role is not tenant specific. don't want to
                //change existing behavior. Need to have business discussion to determine if a user
                //has a non tenant specific role, whether they have access to all tenants in domain.
                //if this turns out to be the case, then we need to change validateToken logic.
                attachRoleToUser(roleService.getComputeDefaultRole(), user, user.getDomainId());
                attachRoleToUser(roleService.getObjectStoreDefaultRole(), user, getNastTenantId(user.getDomainId()));
            }
        }
    }

    @Override
    public void addUserGroupToUser(UserGroup group, User baseUser) {
        logger.info("Adding User: {} to Group: {}", baseUser, group);
        userDao.addUserGroupToUser(group, baseUser);
        logger.info("Added User: {} to Group: {}", baseUser, group);
    }

    @Override
    public void removeUserGroupFromUser(UserGroup group, User baseUser) {
        logger.info("Removing User: {} from Group: {}", baseUser, group);
        userDao.removeUserGroupFromUser(group, baseUser);
        logger.info("Removed User: {} from Group: {}", baseUser, group);
    }


    @Override
    public List<TenantRole> replaceRoleAssignmentsOnUser(User user, RoleAssignments roleAssignments, IdentityUserTypeEnum callerUserType) {
        Validate.notNull(user);
        Validate.notNull(user.getUniqueId());
        Validate.notNull(roleAssignments);
        Validate.notNull(callerUserType);

        if (roleAssignments.getTenantAssignments() == null || CollectionUtils.isEmpty(roleAssignments.getTenantAssignments().getTenantAssignment())) {
            return Collections.emptyList();
        }

        List<TenantRole> tenantRoles = tenantAssignmentService.replaceTenantAssignmentsOnUser(
                user,
                roleAssignments.getTenantAssignments().getTenantAssignment(),
                callerUserType);

        // Send an UPDATE user event when roles change on user.
        atomHopperClient.asyncPost(user, FeedsUserStatusEnum.UPDATE, MDC.get(Audit.GUUID));

        return tenantRoles;
    }

    @Override
    public PaginatorContext<TenantRole> getRoleAssignmentsOnUser(User user, PaginationParams paginationParams) {
        Validate.notNull(user);
        Validate.isTrue(StringUtils.isNotBlank(user.getUniqueId()));
        Validate.notNull(paginationParams);

        return tenantRoleDao.getRoleAssignmentsOnUser(user, paginationParams);
    }

    @Override
    public User getUserAdminByDomain(Domain domain) {
        Validate.notNull(domain);

        return userDao.getUserAdminByDomain(domain);
    }

    @Override
    public User getUserAdminByTenantId(String tenantId) {
        Validate.notNull(tenantId);

        User user = null;

        Tenant tenant = tenantService.getTenant(tenantId);
        // All tenant should have a domainId, but adding check just in case.
        if (tenant != null && tenant.getDomainId() != null) {
            Domain domain = domainService.getDomain(tenant.getDomainId());
            if (domain != null) {
                user = getUserAdminByDomain(domain);
            }
        }

        return user;
    }

    @Override
    public void updateUserForMultiFactor(User user) {
        logger.info("Updating User: {}", user);
        userDao.updateUserAsIs(user);
        logger.info("Updated User: {}", user);
    }


    private void disableUserAdminSubUsers(User user) throws IOException, JAXBException {
        if (authorizationService.hasUserAdminRole(user)) {
            // NOTE: Enabling "feature.enable.user.admin.look.up.by.domain" will cause all users within a domain to be
            // disabled if the user-admin set on domain is disabled. This will occur whether or not there exist another
            // user-admin in the domain.
            List<User> enabledUserAdmins = domainService.getEnabledDomainAdmins(user.getDomainId());
            if (enabledUserAdmins.size() != 0) {
                return;
            }

            Iterable<EndUser> subUsers = identityUserService.getEndUsersByDomainId(user.getDomainId(), User.UserType.VERIFIED);

            for (EndUser subUser : subUsers) {
                if(subUser instanceof User) {
                    User provisionedUser = (User) subUser;
                    if (provisionedUser.getEnabled()) {
                        provisionedUser.setEnabled(false);
                        userDao.updateUser(provisionedUser);
                        tokenRevocationService.revokeAllTokensForEndUser(provisionedUser);
                    }
                } else if(subUser instanceof FederatedUser) {
                    tokenRevocationService.revokeAllTokensForEndUser(subUser);
                }
            }
        }
    }

    /**
     * Checks whether the user is being updated from an enabled state to a disabled state.
     *
     * @param currentUser
     * @param user
     * @return
     */
    private boolean checkIfUserIsBeingDisabled(User currentUser, User user) {
        if (currentUser != null && user != null && user.getEnabled() != null) {
            boolean currentUserEnabled = currentUser.getEnabled();
            boolean userEnabled = user.getEnabled();

            return !userEnabled && userEnabled != currentUserEnabled;
        }
        return false;
    }

    /**
     * Checks whether the user is being updated from a disabled state to an enabled state.
     *
     * @param currentUser
     * @param user
     * @return
     */
    private boolean checkIfUserIsBeingEnabled(User currentUser, User user) {
        if (currentUser != null && user != null && user.getEnabled() != null) {
            boolean currentUserEnabled = currentUser.getEnabled();
            boolean userEnabled = user.getEnabled();

            return userEnabled && userEnabled != currentUserEnabled;
        }
        return false;
    }

    @Override
    public User getUserById(String id) {
        logger.debug(GETTING_USER, id);
        User user = userDao.getUserById(id);
        logger.debug(GOT_USER, user);
        return user;
    }

    /**
     *
     * @deprecated use IdentityUserService services instead as it supports federated users
     * @param id
     * @return
     */
    @Deprecated
    @Override
    public User checkAndGetUserById(String id) {
        User user = getUserById(id);

        if (user == null) {
            String errMsg = String.format(ERROR_MSG_USER_S_NOT_FOUND, id);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        return user;
    }

    // TODO: Refactor this. Using instance of here makes this way too complex.
    @Override
    public BaseUser getUserByScopeAccess(ScopeAccess scopeAccess, boolean checkUserDisabled) {
        BaseUser user = null;
        if (scopeAccess instanceof RackerScopeAccess) {
            RackerScopeAccess rackerScopeAccess = (RackerScopeAccess) scopeAccess;
            user = getRackerByRackerId((rackerScopeAccess.getRackerId()));
        }
        else if (scopeAccess instanceof ImpersonatedScopeAccess) {
            ImpersonatedScopeAccess impersonatedScopeAccess = (ImpersonatedScopeAccess) scopeAccess;
            if (impersonatedScopeAccess.getRackerId() != null) {
                Racker impersonatingRacker = getRackerByRackerId(impersonatedScopeAccess.getRackerId());
                impersonatingRacker.setRackerId(impersonatingRacker.getRackerId());
                impersonatingRacker.setUsername(impersonatingRacker.getUsername());
                user = impersonatingRacker;
                ((Racker)user).setEnabled(true);
            } else {
                user = identityUserService.getEndUserById(impersonatedScopeAccess.getUserRsId());
            }
        } else if (scopeAccess instanceof UserScopeAccess) {
            UserScopeAccess userScopeAccess = (UserScopeAccess) scopeAccess;
            if (userScopeAccess.isDelegationToken()) {
                EndUser realUser = identityUserService.getEndUserById(userScopeAccess.getUserRsId());
                DelegationAgreement delegationAgreement = delegationService.getDelegationAgreementById(userScopeAccess.getDelegationAgreementId());
                if (realUser != null && delegationAgreement != null) {
                    DomainSubUserDefaults domainSubUserDefaults = createSubUserService.calculateDomainSubUserDefaults(delegationAgreement.getDomainId());
                    user = new ProvisionedUserDelegate(domainSubUserDefaults, delegationAgreement, realUser);
                }
            } else if (CollectionUtils.isNotEmpty(userScopeAccess.getAuthenticatedBy()) && userScopeAccess.getAuthenticatedBy().contains(AUTHENTICATED_BY_FEDERATION)) {
                //will be a federated user  (FederatedUser)
                user = federatedUserDao.getUserByToken(userScopeAccess);
            } else {
                //will be a "provisioned" user (User)
                user = identityUserService.getEndUserById(userScopeAccess.getUserRsId());
            }
        } else {
            throw new BadRequestException("Invalid getUserByScopeAccess, scopeAccess cannot provide information to get a user");
        }
        if (user == null) {
            throw new NotFoundException(ERROR_MSG_TOKEN_NOT_FOUND);
        }

        if ( checkUserDisabled ){
           checkUserDisabled(user);
        }

        return user;
    }

    public void checkUserDisabled(BaseUser user) {
        String exMsg = "Token not found.";
        if( user.isDisabled() ){
            throw new NotFoundException(exMsg);
        }

        Domain domain = domainService.getDomain(user.getDomainId());

        if( domain != null && !domain.getEnabled() ) {
            throw new NotFoundException(exMsg);
        }
    }

    @Override
    public boolean userDisabledByTenants(EndUser user) {
        IdentityUserTypeEnum userType = authorizationService.getIdentityTypeRoleAsEnum(user);
        return userType != null && !userType.hasAtLeastIdentityAdminAccessLevel() && tenantService.allTenantsDisabledForUser(user);
    }

    @Override
    public BaseUser getUserByScopeAccess(ScopeAccess scopeAccess) {
        return getUserByScopeAccess(scopeAccess, true);
    }

    @Override
    public void addBaseUrlToUser(String baseUrlId, User user) {
        CloudBaseUrl baseUrl = endpointService.getBaseUrlById(baseUrlId);
        String tenantId;
        if (baseUrl.getOpenstackType().equals("NAST")) {
            tenantId = user.getNastId();
        } else {
            tenantId = String.valueOf(user.getMossoId());
        }

        Tenant tenant = tenantService.getTenant(tenantId);

        // Check for existing BaseUrl
        if (tenant.getBaseUrlIds() != null && tenant.getBaseUrlIds().size() != 0) {
            for (String bId : tenant.getBaseUrlIds()) {
                if (bId.equals(String.valueOf(baseUrl.getBaseUrlId()))) {
                    throw new BadRequestException("BaseUrl already exists.");
                }
            }
        }

        tenant.getBaseUrlIds().add(String.valueOf(baseUrl.getBaseUrlId()));
        this.tenantService.updateTenant(tenant);
    }

    @Override
    public void removeBaseUrlFromUser(String baseUrlId, User user) {
        CloudBaseUrl baseUrl = endpointService.getBaseUrlById(baseUrlId);
        String tenantId;
        if (baseUrl.getOpenstackType().equals("NAST")) {
            tenantId = user.getNastId();
        } else {
            tenantId = String.valueOf(user.getMossoId());
        }

        Tenant tenant = this.tenantService.getTenant(tenantId);
        tenant.getBaseUrlIds().remove(String.valueOf(baseUrl.getBaseUrlId()));
        this.tenantService.updateTenant(tenant);
    }

    @Override
    public void addGroupToUser(String groupId, String userId) {
        userDao.addGroupToUser(userId, groupId);
    }

    @Override
    public void deleteGroupFromUser(String groupId, String userId) {
        userDao.deleteGroupFromUser(groupId, userId);
    }

    @Override
    public Iterable<Group> getGroupsForUser(String userId) {
        return userDao.getGroupsForUser(userId);
    }

    @Override
	public boolean isUserInGroup(String userId, String groupId) {
        for (Group currentGroup : getGroupsForUser(userId)) {
            if (currentGroup.getGroupId().equals(groupId)) {
                return true;
            }
        }
        return false;
	}

    protected List<User> filterUsersForRole(List<User> users, String roleId) {
        List<User> result = new ArrayList<User>();
        for (User user : users) {
            List<TenantRole> roles = tenantService.getGlobalRolesForUser(user);
            if (user.getRoles() != null) {
                roles.addAll(user.getRoles());
            }

            for (TenantRole tenantRole : roles) {
                if (tenantRole.getRoleRsId().equals(roleId)) {
                    result.add(user);
                    break;
                }
            }
        }
        return result;
    }

    public void setScopeAccessService(ScopeAccessService scopeAccessService) {
        this.scopeAccessService = scopeAccessService;
    }

    @Override
    public Iterable<User> getUsersWithDomain(String domainId) {
        return this.userDao.getUsersByDomain(domainId);
    }

    @Override
    public void setTenantService(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    public void setEndpointService(EndpointService endpointService) {
        this.endpointService = endpointService;
    }

    public void setAuthorizationService(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    public void setDomainService(DomainService domainService) {
        this.domainService = domainService;
    }

    @Override
    public void reEncryptUsers() {
        int offset = 0;
        int limit = 50;

        PaginatorContext<User> context = userDao.getUsersToReEncrypt(offset, limit);

        while(offset < context.getTotalRecords()) {
            for (User user : context.getValueList()) {
                userDao.updateUserEncryption(user.getId());
            }

            offset += (limit - context.getValueList().size());
            context = userDao.getUsersToReEncrypt(offset, limit);
        }
    }

    @Override
    public void setCryptHelper(CryptHelper cryptHelper) {
        this.cryptHelper = cryptHelper;
    }

    @Override
    public void setPropertiesService(PropertiesService propertiesService) {
        this.propertiesService = propertiesService;
    }

    @Override
    public void validateUserIsEnabled(BaseUser user) {
        if (user != null) {
            if (user.isDisabled()) {
                logger.error(user.getUsername());
                throw new UserDisabledException("User '" + user.getUsername() +"' is disabled.");
            }
            if (user.getDomainId() != null) {
                Domain domain = domainService.getDomain(user.getDomainId());
                if (domain != null && !domain.getEnabled()) {
                    logger.error(user.getUsername());
                    throw new UserDisabledException("User '" + user.getUsername() +"' is disabled.");
                }
            }
        }
    }

    private static volatile Date FEDERATED_USERS_DELETION_LOCK;

    @Override
    public void expiredFederatedUsersDeletion(FederatedUsersDeletionRequest request, FederatedUsersDeletionResponse response) {
        response.setId((int) (10000 * Math.random()));
        int deleted = 0;
        int errors = 0;

        boolean check;
        synchronized (this) {
            check = FEDERATED_USERS_DELETION_LOCK == null || System.currentTimeMillis() - FEDERATED_USERS_DELETION_LOCK.getTime() > identityConfig.getReloadableConfig().getFederatedDeletionTimeout();
            FEDERATED_USERS_DELETION_LOCK = check ? new Date() : FEDERATED_USERS_DELETION_LOCK;
        }
        if (check) {
            final int repeat = request.getMax() == null || request.getMax() < 0 ? identityConfig.getReloadableConfig().getFederatedDeletionMaxCount() : request.getMax();
            final long delay = request.getDelay() == null || request.getDelay() < 0 ? 0 : request.getDelay();

            for (int i = 0; i < Math.min(repeat, identityConfig.getReloadableConfig().getFederatedDeletionMaxCount()); i++) {
                try {
                    Thread.sleep(Math.min(delay, identityConfig.getReloadableConfig().getFederatedDeletionMaxDelay()));
                    final FederatedUser federatedUser = federatedUserDao.getSingleExpiredFederatedUser();
                    if (federatedUser != null) {
                        // Delete federated user
                        identityUserService.deleteUser(federatedUser);
                        //send atom hopper feed showing deletion of this user
                        atomHopperClient.asyncPost(federatedUser, FeedsUserStatusEnum.DELETED, MDC.get(Audit.GUUID));
                        deleted++;
                    } else {
                        i = repeat;
                    }
                } catch (Exception e) {
                    errors++;
                    final String uuid = response.getId() + "-" + (UUID.randomUUID().toString().replaceAll("-", ""));
                    response.getError().add(uuid);
                    logger.error("[" + uuid + "] Error deleting expiring federated user.", e);
                }
            }
            synchronized (this) {
                FEDERATED_USERS_DELETION_LOCK = null;
            }
        } else {
            logger.error("This node is already processing another federation users deletion since " + FEDERATED_USERS_DELETION_LOCK.toString());
            errors = 1;
        }

        response.setErrors(errors);
        response.setDeleted(deleted);
    }

    void validateUserStatus(UserAuthenticationResult authenticated ) {
        User user = (User)authenticated.getUser();
        boolean isAuthenticated = authenticated.isAuthenticated();
        if (user != null && isAuthenticated) {
            validateUserIsEnabled(user);
            logger.debug("User {} authenticated == {}", user.getUsername(), isAuthenticated);
        }
    }

    private void setPasswordIfNotProvided(User user) {
        if (StringUtils.isBlank(user.getPassword())) {
            Password.generateRandom(false, user);
        }
    }

    private void setApiKeyIfNotProvided(User user) {
        if (StringUtils.isBlank(user.getApiKey())) {
            user.setApiKey(UUID.randomUUID().toString().replaceAll("-", ""));
        }
    }

    private void setRegionIfNotProvided(User user, Domain domain) {
        String cloudRegion;
        if (StringUtils.isBlank(user.getRegion())) {
            if (identityConfig.getRepositoryConfig().shouldUseDomainTypeOnNewUserCreation()) {
                cloudRegion = inferCloudBasedOnDomainType(domain.getType());
            } else {
                cloudRegion = identityConfig.getStaticConfig().getCloudRegion();
            }

            Region region = cloudRegionService.getDefaultRegion(cloudRegion);
            if (region == null) {
                throw new IllegalStateException("default cloud region not found for: " + cloudRegion);
            }
            user.setRegion(region.getName());
        }
    }

    /**
     * If the domain type is RACKSPACE_CLOUD_US, the cloud region is US.
     * If the domain type is RACKSPACE_CLOUD_UK, the cloud region is UK.
     * If the domain type is something else (including null), the cloud region is US.
     */
    public String inferCloudBasedOnDomainType(String domainType) {
        if (DomainType.RACKSPACE_CLOUD_US.getName().equalsIgnoreCase(domainType)) {
            return CloudRegion.US.getName();
        }
        if (DomainType.RACKSPACE_CLOUD_UK.getName().equalsIgnoreCase(domainType)) {
            return CloudRegion.UK.getName();
        }
        return CloudRegion.US.getName();
    }

    private Domain createDomainIfItDoesNotExist(String domainId) {
        Domain domain = null;
        if (StringUtils.isNotBlank(domainId)) {
            domain = domainService.getDomain(domainId);
            if (domain == null) {
                if (!authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(
                            IdentityUserTypeEnum.SERVICE_ADMIN.getRoleName(),
                            IdentityRole.IDENTITY_RS_DOMAIN_ADMIN.getRoleName())) {
                    throw new ForbiddenException(NOT_AUTHORIZED_MSG);
                }
                domain = domainService.createDomainWithFallbackGet(domainId);
            }
        }
        return domain;
    }

    private Domain createDomainUserInCreateOneCall(String domainId, boolean isUserAdminCreateInOneCall) {
        Domain domain = null;
        if (StringUtils.isNotBlank(domainId)) {
            if (domainId.equals(identityConfig.getReloadableConfig().getTenantDefaultDomainId())) {
                throw new ForbiddenException(ERROR_MSG_NEW_ACCOUNT_IN_DEFAULT_DOMAIN );
            }
            domain = domainService.getDomain(domainId);
            if (domain != null) {
                if (!domain.getEnabled()) {
                    throw new ForbiddenException(ERROR_MSG_NEW_ACCOUNT_IN_DISABLED_DOMAIN);
                }

                if (isUserAdminCreateInOneCall  && Iterables.size(domainService.getDomainAdmins(domainId)) != 0) {
                    throw new ForbiddenException(ERROR_MSG_NEW_ACCOUNT_IN_DOMAIN_WITH_USERS);
                }

                if (StringUtils.isBlank(domain.getType())) {
                    domain.setType(domainService.inferDomainTypeForDomainId(domain.getDomainId()));
                    logger.info("Updating existing domain with ID {} to have domain type {}", domain.getType());
                    domainService.updateDomain(domain);
                }
            } else {
                if (!authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(IdentityRole.IDENTITY_RS_DOMAIN_ADMIN.getRoleName())) {
                    throw new ForbiddenException(NOT_AUTHORIZED_MSG);
                }
                domain = domainService.createDomainWithFallbackGet(domainId);
            }
        }
        return domain;
    }

    private void verifyCallerCanCreateTenants(User user, boolean provisionMossoAndNast) {
        if (identityConfig.getReloadableConfig().isUseRoleForTenantManagementEnabled() && oneUserCallWouldCreateTenants(user, provisionMossoAndNast)) {
            authorizationService.verifyEffectiveCallerHasRoleByName(IdentityRole.IDENTITY_RS_TENANT_ADMIN.getRoleName());
        }
    }

    private boolean oneUserCallWouldCreateTenants(User user, boolean provisionMossoAndNast) {
        for (TenantRole role : user.getRoles()) {
            for (String tenantId : role.getTenantIds()) {
                Tenant tenant = tenantService.getTenant(tenantId);
                if (tenant == null) {
                    return true;
                }
            }
        }

        if (provisionMossoAndNast) {
            String mossoId = user.getDomainId();
            String nastId = getNastTenantId(user.getDomainId());

            Tenant mossoTenant = tenantService.getTenant(mossoId);
            if (mossoTenant == null) {
                return true;
            }
            Tenant nastTenant = tenantService.getTenant(nastId);
            if (nastTenant == null) {
                return true;
            }
        }

        return false;
    }

    private void verifyUserTenantsInCreateOneCall(User user) {
        for (TenantRole role : user.getRoles()) {
            for (String tenantId : role.getTenantIds()) {
                Tenant tenant = tenantService.getTenant(tenantId);
                if (tenant != null) {

                    if (!user.getDomainId().equals(tenant.getDomainId())) {
                        throw new ForbiddenException(ERROR_MSG_NEW_ACCOUNT_EXISTING_TENANT_DIFFERENT_DOMAIN);
                    }
                    if (domainService.getDomain(tenant.getDomainId()) == null) {
                        throw new ForbiddenException(ERROR_MSG_NEW_ACCOUNT_EXISTING_TENANT_WITH_USERS);
                    }
                }
            }
        }
    }

    /**
     * creates default tenants in the new domain
     *
     * @param domain
     */
    private void createDefaultDomainTenantsIfNecessary(Domain domain) {
        if (domain != null && domainService.getDomainAdmins(domain.getDomainId()).size() == 0) {
            //for now the default mosso tenant id will be the domain id
            //for now we will create a nast tenant as well. This can be removed once tenant aliases is in place
            //no longer need to call nast xml rpc service, as cloud files will lazy provision the cloud containers.
            String mossoId = domain.getDomainId();
            String nastId = getNastTenantId(domain.getDomainId());

            Tenant mossoTenant = createTenant(mossoId, domain, MOSSO_BASE_URL_TYPE);
            Tenant nastTenant = createTenant(nastId, domain, NAST_BASE_URL_TYPE);

            createTenantForDomain(mossoTenant);
            createTenantForDomain(nastTenant);
        }
    }

    private void createDefaultDomainTenantsInCreateOneCall(Domain domain) {
        String mossoId = domain.getDomainId();
        String nastId = getNastTenantId(domain.getDomainId());

        try {
            Tenant mossoTenant = createTenant(mossoId, domain, MOSSO_BASE_URL_TYPE);
            if (identityConfig.getReloadableConfig().shouldSetDefaultTenantTypeOnCreation()) {
                mossoTenant.getTypes().add(TENANT_TYPE_CLOUD);
            }
            createTenantForDomain(mossoTenant);
        } catch (DuplicateException e) {
            Tenant storedMossoTenant = tenantService.getTenant(mossoId);
            attachEndpointsToTenant(storedMossoTenant, endpointService.getBaseUrlsByBaseUrlType(MOSSO_BASE_URL_TYPE), domain);
            tenantService.updateTenant(storedMossoTenant);
        }

        try {
            Tenant nastTenant = createTenant(nastId, domain, NAST_BASE_URL_TYPE);
            if (identityConfig.getReloadableConfig().shouldSetDefaultTenantTypeOnCreation()) {
                nastTenant.getTypes().add(TENANT_TYPE_FILES);
            }
            createTenantForDomain(nastTenant);
        } catch (DuplicateException e) {
            Tenant storedNastTenant = tenantService.getTenant(nastId);
            attachEndpointsToTenant(storedNastTenant, endpointService.getBaseUrlsByBaseUrlType(NAST_BASE_URL_TYPE), domain);
            tenantService.updateTenant(storedNastTenant);
        }
    }

    private void createTenantForDomain(Tenant tenant) {
        tenantService.addTenant(tenant);
        domainService.addTenantToDomain(tenant.getTenantId(), tenant.getDomainId());
    }

    private Tenant createTenant(String tenantId, Domain domain, String baseUrlType) {
        Tenant tenant = new Tenant();
        tenant.setTenantId(tenantId);
        tenant.setName(tenantId);
        tenant.setDisplayName(tenantId);
        tenant.setEnabled(true);
        tenant.setDomainId(domain.getDomainId());
        attachEndpointsToTenant(tenant, endpointService.getBaseUrlsByBaseUrlType(baseUrlType), domain);
        return tenant;
    }

    private void attachEndpointsToTenant(Tenant tenant, List<CloudBaseUrl> baseUrls, Domain domain) {
        if (identityConfig.getRepositoryConfig().shouldUseDomainTypeOnNewUserCreation() && tenant.getDomainId() != null) {
            for (CloudBaseUrl baseUrl : baseUrls) {
                if (endpointService.doesBaseUrlBelongToCloudRegion(baseUrl, domain) && baseUrl.getDef() != null && baseUrl.getDef()) {
                    tenant.getBaseUrlIds().add(baseUrl.getBaseUrlId());
                    addV1DefaultToTenant(tenant, baseUrl, domain);
                }
            }
        } else {
            // Legacy logic
            for (CloudBaseUrl baseUrl : baseUrls) {
                if(endpointService.doesBaseUrlBelongToCloudRegion(baseUrl) && baseUrl.getDef() != null && baseUrl.getDef()){
                    tenant.getBaseUrlIds().add(baseUrl.getBaseUrlId());
                    addV1DefaultToTenant(tenant, baseUrl);
                }
            }
        }
    }

    private void addV1DefaultToTenant(Tenant tenant, CloudBaseUrl baseUrl) {
        List<Object> v1defaultList = new ArrayList<Object>();
        String baseUrlId = String.valueOf(baseUrl.getBaseUrlId());
        String baseUrlType = baseUrl.getBaseUrlType();

        if(baseUrlType.equals(MOSSO)) {
            v1defaultList = config.getList("v1defaultMosso");
        } else if(baseUrlType.equals(NAST)) {
            v1defaultList = config.getList("v1defaultNast");
        }

        for (Object v1defaultItem : v1defaultList) {
            if (v1defaultItem.equals(baseUrlId) && baseUrl.getDef()) {
                baseUrl.setV1Default(true);
                tenant.getV1Defaults().add(baseUrlId);
            }
        }
    }

    /**
     * Sets the v1 defaults on the tenant based on the domain's region.
     */
    private void addV1DefaultToTenant(Tenant tenant, CloudBaseUrl baseUrl, Domain domain) {
        List<Object> v1defaultList = new ArrayList<Object>();
        String baseUrlId = String.valueOf(baseUrl.getBaseUrlId());
        String baseUrlType = baseUrl.getBaseUrlType();

        if(baseUrlType.equals(MOSSO) && domain.getType().equalsIgnoreCase(DomainType.RACKSPACE_CLOUD_US.getName())) {
            v1defaultList = new ArrayList<>(identityConfig.getReloadableConfig().getV1DefaultCloudEndpointsUs());
        } else if(baseUrlType.equals(NAST) && domain.getType().equalsIgnoreCase(DomainType.RACKSPACE_CLOUD_US.getName())) {
            v1defaultList = new ArrayList<>(identityConfig.getReloadableConfig().getV1DefaultFilesEndpointsUs());
        } else if(baseUrlType.equals(MOSSO) && domain.getType().equalsIgnoreCase(DomainType.RACKSPACE_CLOUD_UK.getName())) {
            v1defaultList = new ArrayList<>(identityConfig.getReloadableConfig().getV1DefaultCloudEndpointsUk());
        } else if(baseUrlType.equals(NAST) && domain.getType().equalsIgnoreCase(DomainType.RACKSPACE_CLOUD_UK.getName())) {
            v1defaultList = new ArrayList<>(identityConfig.getReloadableConfig().getV1DefaultFilesEndpointsUk());
        }

        for (Object v1defaultItem : v1defaultList) {
            if (v1defaultItem.equals(baseUrlId) && baseUrl.getDef()) {
                baseUrl.setV1Default(true);
                tenant.getV1Defaults().add(baseUrlId);
            }
        }
    }

    private void assignUserRoles(User user, boolean isCreateUserInOneCall) {
        boolean sendFeedEvent = false;
        try {
            for (TenantRole role : user.getRoles()) {
                ClientRole roleObj = roleService.getRoleByName(role.getName());

                TenantRole tenantRole = new TenantRole();
                tenantRole.setRoleRsId(roleObj.getId());
                tenantRole.setClientId(roleObj.getClientId());
                tenantRole.setName(roleObj.getName());
                tenantRole.setUserId(user.getId());
                tenantRole.getTenantIds().addAll(role.getTenantIds());

                try {
                    tenantService.addTenantRoleToUser(user, tenantRole, false);
                    sendFeedEvent = true;
                } catch (ClientConflictException e) {
                    if (!isCreateUserInOneCall) {
                        throw e;
                    }
                }
            }
        } finally {
            // Send user feed event if at least one tenantRole was added to the user.
            if (sendFeedEvent) {
                atomHopperClient.asyncPost(user, FeedsUserStatusEnum.ROLE, MDC.get(Audit.GUUID));
            }
        }
    }

    private void checkMaxNumberOfUsersInDomain(String domainId) {
        if (StringUtils.isNotBlank(domainId)) {
            Iterable<User> users = getUsersWithDomain(domainId);
            int numUsers = 0;
            int maxNumberOfUsersInDomain = identityConfig.getStaticConfig().getMaxNumberOfUsersInDomain();

            for (Iterator i = users.iterator(); i.hasNext();) {
                i.next();
                numUsers++;

                if (numUsers >= maxNumberOfUsersInDomain) {
                    String errMsg = String.format(ERROR_PATTERN_MAX_NUM_USERS_IN_DOMAIN, maxNumberOfUsersInDomain);
                    throw new BadRequestException(errMsg);
                }
            }
        }
    }

    /**
     * The format of the nast tenant id is XY, where X is determined by the configuration property NAST_TENANT_PREFIX_PROP
     * and Y is the domainId.
     *
     * Returns null if the supplied domainId is null.
     *
     * @param domainId
     * @return
     */
    public String getNastTenantId(String domainId)  {
        String prefix = getNastTenantPrefix();
        return StringUtils.isNotBlank(domainId) ? prefix + domainId : null;
    }

    private String getNastTenantPrefix() {
        return identityConfig.getStaticConfig().getNastTenantPrefix();
    }

    private void attachRoleToUser(ClientRole role, User user) {
        attachRoleToUser(role, user, null);
    }

    private void attachRoleToUser(ClientRole role, User user, String tenantId) {
        TenantRole tenantRole = new TenantRole();
        tenantRole.setName(role.getName());
        if (tenantId != null) {
            tenantRole.getTenantIds().add(tenantId);
        }

        if (!user.getRoles().contains(tenantRole)) {
            user.getRoles().add(tenantRole);
        }
    }

    boolean isUserAdmin(User user) {
        boolean hasRole = false;
        for (TenantRole tenantRole : user.getRoles()) {
            String name = tenantRole.getName();
            if (name.equals("identity:user-admin")) {
                hasRole = true;
            }
        }
        return hasRole;
    }

    boolean isRackerAuthAllowed() {
        return identityConfig.getStaticConfig().isRackerAuthAllowed();
    }

    int getLdapPagingOffsetDefault() {
        return config.getInt("ldap.paging.offset.default");
    }

    int getLdapPagingLimitDefault() {
        return config.getInt("ldap.paging.limit.default");
    }

    private String getIdmClientId() {
        return config.getString("idm.clientId");
    }

    private String getCloudAuthClientId() {
        return config.getString("cloudAuth.clientId");
    }

    private String getRackspaceCustomerId() {
        return config.getString("rackspace.customerId");
    }

    private boolean getDomainRestrictedToOneUserAdmin() {
        return config.getBoolean("domain.restricted.to.one.user.admin.enabled", false);
    }

    public void setIdentityConfig(IdentityConfig identityConfig) {
        this.identityConfig = identityConfig;
    }
}
