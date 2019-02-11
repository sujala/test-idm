package com.rackspace.idm.validation;


import com.rackspace.idm.ErrorCodes;
import com.rackspace.idm.api.resource.cloud.v20.DefaultRegionService;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.PatternDao;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.DuplicateUsernameException;
import com.rackspacecloud.docs.auth.api.v1.BaseURL;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 11/30/12
 * Time: 1:16 PM
 * To change this template use File | Settings | File Templates.
 */
@Component
public class Validator {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String FEATURE_VALIDATE_SUBUSER_DEFAULTREGION_ENABLED_PROP_NAME="feature.validate.subuser.defaultregion.enabled";
    private static final boolean FEATURE_VALIDATE_SUBUSER_DEFAULTREGION_ENABLED_DEFAULT_VALUE=true;

    @Autowired
    PatternDao ldapPatternRepository;

    @Autowired
    DefaultRegionService defaultRegionService;

    @Autowired
    UserService userService;

    @Autowired
    RoleService roleService;

    @Autowired
    GroupService groupService;

    @Autowired
    Configuration config;

    @Autowired
    IdentityConfig identityConfig;

    @Autowired
    PasswordBlacklistService passwordBlacklistService;

    static final String USERNAME="username";
    static final String ALPHANUMERIC="alphanumeric";
    static final String PASSWORD="password";
    static final String EMAIL = "email";

    public boolean isEmpty(String str){
        return StringUtils.isEmpty(str);
    }

    public boolean isBlank(String str){
        return StringUtils.isBlank(str);
    }

    public boolean isUsernameValid(String username){
        return checkPattern(USERNAME, username);
    }

    public boolean isAlphaNumeric(String value){
        return checkPattern(ALPHANUMERIC, value);
    }

    public void assertEmailValid(String email) {
        checkPattern(EMAIL, email);
    }

    public boolean isEmailValid(String email) {
        return checkPatternNoException(EMAIL, email);
    }

    public void validate11User(com.rackspacecloud.docs.auth.api.v1.User user) {
        validateMossoId(user.getMossoId());
    }

    public void validateUser(com.rackspace.idm.domain.entity.User user) {
        validateUsername(user.getUsername());
        validatePassword(user.getPassword());
        validateEmail(user.getEmail());
        validateUserForCreateOrUpgrade(user);
    }


    public void validateUserForCloudUpgrade(com.rackspace.idm.domain.entity.User user) {
        validateUserForCreateOrUpgrade(user);
    }

    private void validateUserForCreateOrUpgrade(com.rackspace.idm.domain.entity.User user) {
        /*
        don't validate the user's default region for subusers. This is required due to D-18002 https://www15.v1host.com/RACKSPCE/defect.mvc/Summary?oidToken=Defect:1066346
        where UK user-admins need to be able to create subusers on US IDM. In this case the defaultRegion on the UK admin
        will be something like "LON" which is NOT a valid region for US IDM (though it is for UK IDM).

        Also, currently the defaultRegion of all subusers will get set to the defaultRegion of the associated user-admin or creator (see B-58588 and D-18046).
        Since we can assume the user-admin has a valid default region, we don't need to validate the subuser's default region at this time. When
        D-18046 is addressed, the default region on subuser's will need to be verified once again.

        Keeping this logic here rather than moving logic higher to prevent changing the order of validation checks (e.g. an invalid username will
        be thrown before an invalid region).
         */
        if (validateSubUserDefaultRegion() || !isUserASubUser(user)) {
            //if we're validating subusers, then we're validating ALL users so can short circuit the check whether the user
            //is a subuser if we need to validate the region anyways.
            validateDefaultRegion(user.getRegion());
        }
        validateRoles(user.getRoles());
        validateGroups(user.getRsGroupId());
    }

    private boolean validateSubUserDefaultRegion() {
        return config.getBoolean(FEATURE_VALIDATE_SUBUSER_DEFAULTREGION_ENABLED_PROP_NAME, FEATURE_VALIDATE_SUBUSER_DEFAULTREGION_ENABLED_DEFAULT_VALUE);
    }

    private boolean isUserASubUser(com.rackspace.idm.domain.entity.User user) {
        String subUserRoleName = IdentityUserTypeEnum.DEFAULT_USER.getRoleName();
        List<TenantRole> userRoles = user.getRoles();
        for (TenantRole userRole : userRoles) {
            if (subUserRoleName != null && subUserRoleName.equals(userRole.getName())) {
               return true;
            }
        }
        return false;
    }

    public boolean validatePasswordForCreateOrUpdate(String password) {
        boolean isPatternCheckOk = checkPattern(PASSWORD, password);

        // Before setting password ensure password is not blacklisted
        // which mean it is not a publicly compromised password.
        if (identityConfig.getReloadableConfig().isPasswordBlacklistValidationEnabled()) {
            validatePasswordIsNotBlacklisted(password);
        }
        return isPatternCheckOk;
    }

    private boolean checkPattern(String pattern, String value) {
        logger.warn("Checking regex pattern");
        com.rackspace.idm.domain.entity.Pattern tempPattern = ldapPatternRepository.getPattern(pattern);
        Pattern pat = null;
        try{
            pat = Pattern.compile(tempPattern.getRegex());
        }catch (Exception ex){
            String errMsg = String.format("'%s' is not a valid regular expression.",tempPattern.getRegex());
            logger.warn(errMsg);
            throw new IllegalStateException(errMsg);
        }
        if(value == null || !pat.matcher(value).matches()){
            throw new BadRequestException(tempPattern.getErrMsg());
        }

        return true;
    }

    private boolean checkPatternNoException(String pattern, String value) {
        logger.info("Checking regex pattern");
        com.rackspace.idm.domain.entity.Pattern tempPattern = ldapPatternRepository.getPattern(pattern);

        Pattern pat;
        try {
            pat = Pattern.compile(tempPattern.getRegex());
        } catch (Exception ex) {
            String errMsg = String.format("'%s' is not a valid regular expression.", tempPattern.getRegex());
            logger.error(errMsg);
            throw new IllegalStateException(errMsg);
        }

        if(value == null || !pat.matcher(value).matches()) {
            return false;
        }

        return true;
    }

    public void validateUsername(String username) {
        if (StringUtils.isBlank(username)) {
            throw new BadRequestException("Username is not specified");
        }

        if (!userService.isUsernameUnique(username)) {
            logger.warn("Couldn't add user {} because username already taken", username);
            throw new DuplicateUsernameException("Username unavailable within Rackspace system. Please try another.");
        }

        checkPattern(USERNAME, username);
    }

    private void validateEmail(String email) {
        if (!isEmpty(email)) {
            checkPattern(EMAIL, email);
        }
    }

    private void validateDefaultRegion(String defaultRegion) {
        if (!isEmpty(defaultRegion)) {
            defaultRegionService.validateDefaultRegion(defaultRegion);
        }
    }

    private void validatePassword(String password) {
        if (!isEmpty(password)) {
            checkPattern(PASSWORD, password);
        }

        if (identityConfig.getReloadableConfig().isPasswordBlacklistValidationEnabled()) {
            validatePasswordIsNotBlacklisted(password);
        }
    }

    private void validateRoles(List<TenantRole> roles) {
        if (roles != null && roles.size() > 0) {
            Set<String> roleNames = new HashSet<String>();

            for (TenantRole tenantRole : roles) {
                if (roleService.getRoleByName(tenantRole.getName()) == null) {
                    throw new BadRequestException("role '" + tenantRole.getName() + "' does not exist");
                }

                if (roleNames.contains(tenantRole.getName())) {
                    throw new BadRequestException("role '" + tenantRole.getName() + "' specified more than once");
                }

                roleNames.add(tenantRole.getName());
            }
        }
    }

    private void validateGroups(Set<String> groupIds) {
        if (groupIds != null && !groupIds.isEmpty()) {
            for (Iterator<String> i = groupIds.iterator(); i.hasNext();) {
                String groupId = i.next();
                if (groupService.getGroupById(groupId) == null) {
                    throw new BadRequestException("group '" + groupId + "' does not exist");
                }
            }
        }
    }

    private void validateMossoId(Integer mossoId) {
        if (mossoId == null || mossoId.equals(0)) {
            String errorMsg = "Expecting mossoId";
            logger.warn(errorMsg);
            throw new BadRequestException(errorMsg);
        }

        User user = userService.getUserByTenantId(String.valueOf(mossoId));
        if (user != null) {
            throw new BadRequestException("User with Mosso Account ID: " + mossoId + " already exists.");
        }
    }

    public void validateBaseUrl(BaseURL baseUrl) {
        if (StringUtils.isBlank(baseUrl.getServiceName())) {
            String errMsg = "'serviceName' is a required attribute";
            logger.warn(errMsg);
            throw new BadRequestException(errMsg);
        }
        if (StringUtils.isBlank(baseUrl.getPublicURL())) {
            String errMsg = "'publicURL' is a required attribute";
            logger.warn(errMsg);
            throw new BadRequestException(errMsg);
        }
    }

    public void validatePasswordIsNotBlacklisted(String password){
        if (passwordBlacklistService.isPasswordInBlacklist(password)) {

            throw new BadRequestException(ErrorCodes.ERROR_CODE_BLACKLISTED_PASSWORD_MSG,
                    ErrorCodes.ERROR_CODE_BLACKLISTED_PASSWORD);
        }
    }
}
