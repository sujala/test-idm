package com.rackspace.idm.api.resource.cloud;


import com.rackspace.idm.domain.dao.impl.LdapPatternRepository;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspacecloud.docs.auth.api.v1.User;
import org.apache.commons.lang.StringUtils;
import org.hibernate.validator.constraints.impl.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
    @Autowired
    LdapPatternRepository ldapPatternRepository;

    private EmailValidator emailValidator = new EmailValidator();
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    static final String USERNAME="username";
    static final String PHONE="phone";
    static final String ALPHANUMERIC="alphanumeric";
    static final String PASSWORD="password";
    static final String TOKEN = "token";
    static final String EMAIL = "email";

    static final String USER_ID_EMPTY_MSG = "User cannot be empty.";
    static final String USER_NULL_MSG = "User can not be null.";

    public boolean isEmpty(String str){
        return StringUtils.isEmpty(str);
    }

    public boolean isBlank(String str){
        return StringUtils.isBlank(str);
    }

    public boolean isUsernameValid(String username){
        return checkPattern(USERNAME, username);
    }

    public boolean isPhoneValid(String phone){
         return checkPattern(PHONE, phone);
    }

    public boolean isAlphaNumeric(String value){
        return checkPattern(ALPHANUMERIC, value);
    }

    public boolean isTokenValid(String token){
        return checkPattern(TOKEN, token);
    }

    public boolean isEmailValid(String email) {
        return checkPattern(EMAIL, email);
    }

    public void validate11User(User user) {
        if (user == null) {
            logger.warn(USER_NULL_MSG);
            throw new BadRequestException(USER_NULL_MSG);
        } else if (StringUtils.isBlank(user.getId())) {
            logger.warn(USER_ID_EMPTY_MSG);
            throw new BadRequestException(USER_ID_EMPTY_MSG);
        }
    }

    public void validate20User(org.openstack.docs.identity.api.v2.User user){
        if (StringUtils.isBlank(user.getUsername())) {
            String errorMsg = "Expecting username";
            logger.warn(errorMsg);
            throw new BadRequestException(errorMsg);
        }
        checkPattern(USERNAME, user.getUsername());
        if(!isEmpty(user.getEmail())){
            isEmailValid(user.getEmail());
        }
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
        if(!pat.matcher(value).matches()){
            throw new BadRequestException(tempPattern.getErrMsg());
        }

        return true;
    }

    public void setLdapPatternRepository(LdapPatternRepository ldapPatternRepository){
        this.ldapPatternRepository = ldapPatternRepository;

    }


}
