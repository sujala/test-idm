package com.rackspace.idm.validation;


import com.rackspace.idm.api.resource.cloud.v20.DefaultRegionService;
import com.rackspace.idm.domain.dao.ApplicationRoleDao;
import com.rackspace.idm.domain.dao.GroupDao;
import com.rackspace.idm.domain.dao.impl.LdapPatternRepository;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.DuplicateUsernameException;
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

    @Autowired
    LdapPatternRepository ldapPatternRepository;

    @Autowired
    DefaultRegionService defaultRegionService;

    @Autowired
    UserService userService;

    @Autowired
    ApplicationRoleDao roleDao;

    @Autowired
    GroupDao groupDao;

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

    public boolean isEmailValid(String email) {
        return checkPattern(EMAIL, email);
    }

    public void validate11User(com.rackspacecloud.docs.auth.api.v1.User user) {
        validateMossoId(user.getMossoId());
    }

    public void validateUser(com.rackspace.idm.domain.entity.User user) {
        validateUsername(user.getUsername());
        validatePassword(user.getPassword());
        validateEmail(user.getEmail());
        validateDefaultRegion(user.getRegion());
        validateRoles(user.getRoles());
        validateGroups(user.getRsGroupId());
    }

    public boolean validatePasswordForCreateOrUpdate(String password){
        return checkPattern(PASSWORD, password);
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

    private void validateUsername(String username) {
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
    }

    private void validateRoles(List<TenantRole> roles) {
        if (roles != null && roles.size() > 0) {
            Set<String> roleNames = new HashSet<String>();

            for (TenantRole tenantRole : roles) {
                if (roleDao.getRoleByName(tenantRole.getName()) == null) {
                    throw new BadRequestException("role '" + tenantRole.getName() + "' does not exist");
                }

                if (roleNames.contains(tenantRole.getName())) {
                    throw new BadRequestException("role '" + tenantRole.getName() + "' specified more than once");
                }

                roleNames.add(tenantRole.getName());
            }
        }
    }

    private void validateGroups(HashSet<String> groupIds) {
        if (groupIds != null && !groupIds.isEmpty()) {
            for (Iterator<String> i = groupIds.iterator(); i.hasNext();) {
                String groupId = i.next();
                if (groupDao.getGroupById(groupId) == null) {
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

}
