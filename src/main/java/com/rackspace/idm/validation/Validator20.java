package com.rackspace.idm.validation;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.ImpersonationRequest;
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.NotFoundException;
import org.apache.commons.lang.CharUtils;
import org.apache.commons.lang.StringUtils;
import org.openstack.docs.identity.api.v2.PasswordCredentialsRequiredUsername;
import org.openstack.docs.identity.api.v2.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 8/3/12
 * Time: 4:04 PM
 * To change this template use File | Settings | File Templates.
 */
@Component
public class Validator20 {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    public static final int PASSWORD_MIN_LENGTH = 8;
    public static final int MAX_GROUP_NAME = 200;
    public static final int MAX_GROUP_DESC = 1000;
    @Autowired
    private TenantService tenantService;

    public void validateUsername(String username) {
        if (StringUtils.isBlank(username)) {
            String errorMsg = "Expecting username";
            logger.warn(errorMsg);
            throw new BadRequestException(errorMsg);
        }
        if (username.contains(" ")) {
            String errorMsg = "Username should not contain white spaces";
            logger.warn(errorMsg);
            throw new BadRequestException(errorMsg);
        }
    }

    public void validateUsernameForUpdateOrCreate(String username) {
        Pattern alphaNumberic = Pattern.compile("[a-zA-z0-9]*");
        if (!alphaNumberic.matcher(username).matches()) {
            throw new BadRequestException("Username has invalid characters; only alphanumeric characters are allowed.");
        }
        if (!CharUtils.isAsciiAlpha(username.charAt(0))) {
            throw new BadRequestException("Username must begin with an alphabetic character.");
        }
    }

    public void validateEmail(String email) {
        if (StringUtils.isBlank(email) || !email.matches("[a-zA-Z0-9_\\-\\.\"]+@[a-zA-Z0-9_\\.]+\\.[a-zA-Z]+")) {
            String errorMsg = "Expecting valid email address";
            logger.warn(errorMsg);
            throw new BadRequestException(errorMsg);
        }
    }

    public void validateUserForCreate(User user) {
        validateUsername(user.getUsername());
        validateUsernameForUpdateOrCreate(user.getUsername());
        validateEmail(user.getEmail());
    }

    public void validatePasswordCredentials(PasswordCredentialsRequiredUsername passwordCredentials) {
        String username = passwordCredentials.getUsername();
        String password = passwordCredentials.getPassword();
        validateUsername(username);
        if (StringUtils.isBlank(password)) {
            String errMsg = "Expecting password";
            logger.warn(errMsg);
            throw new BadRequestException(errMsg);
        }
    }

    public void validatePasswordForCreateOrUpdate(String password){
        String errMsg = "Password must be at least 8 characters in length, must contain at least one uppercase letter, one lowercase letter, and one numeric character.";
        if (password.length() < PASSWORD_MIN_LENGTH) {
            logger.warn(errMsg);
            throw new BadRequestException(errMsg);
        }
        if (!password.matches(".*[A-Z].*")) {
            logger.warn(errMsg);
            throw new BadRequestException(errMsg);
        }
        if (!password.matches(".*[a-z].*")) {
            logger.warn(errMsg);
            throw new BadRequestException(errMsg);
        }
        if (!password.matches(".*[0-9].*")) {
            logger.warn(errMsg);
            throw new BadRequestException(errMsg);
        }
    }

    public void validatePasswordCredentialsForCreateOrUpdate(PasswordCredentialsRequiredUsername passwordCredentials){
        validatePasswordCredentials(passwordCredentials);
        validatePasswordForCreateOrUpdate(passwordCredentials.getPassword());
    }

    public void validateApiKeyCredentials(ApiKeyCredentials apiKeyCredentials) {
        validateUsername(apiKeyCredentials.getUsername());
        if (StringUtils.isBlank(apiKeyCredentials.getApiKey())) {
            String errMsg = "Expecting apiKey";
            logger.warn(errMsg);
            throw new BadRequestException(errMsg);
        }
    }

    public void validateImpersonationRequest(ImpersonationRequest impersonationRequest) {
        if (impersonationRequest.getUser() == null) {
            throw new BadRequestException("User cannot be null for impersonation request");
        } else if (impersonationRequest.getUser().getUsername() == null) {
            throw new BadRequestException("Username cannot be null for impersonation request");
        } else if (impersonationRequest.getUser().getUsername().isEmpty() || StringUtils.isBlank(impersonationRequest.getUser().getUsername())) {
            throw new BadRequestException("Username cannot be empty or blank");
        } else if (impersonationRequest.getExpireInSeconds() != null && impersonationRequest.getExpireInSeconds() < 1) {
            throw new BadRequestException("Expire in element cannot be less than 1.");
        }
    }

    public void validateKsGroup(com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group group) {
        String checkName = group.getName();
        if (StringUtils.isBlank(checkName)) {
            throw new BadRequestException("Missing group name");
        }
        if (checkName.length() > MAX_GROUP_NAME) {
            throw new BadRequestException("Group name length cannot exceed 200 characters");
        }
        if (group.getDescription().length() > MAX_GROUP_DESC) {
            throw new BadRequestException("Group description length cannot exceed 1000 characters");
        }
    }

    public void validateGroupId(String groupId) {
        try {
          Integer.parseInt(groupId.trim());
        } catch (Exception e) {
            throw new BadRequestException("Invalid group id");
        }
    }

    public void validateTenantIdInRoles(String tenantId, List<TenantRole> roles) {
        if (!StringUtils.isBlank(tenantId) && !tenantService.isTenantIdContainedInTenantRoles(tenantId, roles)) {
            String errMsg = String.format("Token doesn't belong to Tenant with Id/Name: '%s'", tenantId);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }
    }

    public void setTenantService(TenantService tenantService) {
        this.tenantService = tenantService;
    }
}
