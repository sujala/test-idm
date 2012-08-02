package com.rackspace.idm.api.resource.cloud.v11;

import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.NotAuthenticatedException;
import com.rackspace.idm.exception.NotAuthorizedException;
import com.rackspace.idm.exception.UserDisabledException;
import com.rackspacecloud.docs.auth.api.v1.*;
import com.rackspacecloud.docs.auth.api.v1.Credentials;
import com.rackspacecloud.docs.auth.api.v1.PasswordCredentials;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 6/18/12
 * Time: 10:53 AM
 */
@Component
public class CredentialValidator {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public void validateCredential(Credentials credential, UserService userService){
        if(credential instanceof NastCredentials){
            validateNastCredentials((NastCredentials) credential, userService);
        }
        else if(credential instanceof UserCredentials) {
            validateUserCredentials((UserCredentials) credential);
        }
        else if(credential instanceof PasswordCredentials) {
            validatePasswordCredentials((PasswordCredentials) credential, userService);
        }
        else if(credential instanceof MossoCredentials) {
            validateMossoCredentials((MossoCredentials) credential, userService);
        }
        else {
            String errmsg = "Unknown credential type";
            logger.warn(errmsg);
            throw new BadRequestException(errmsg);
        }
    }

    public void validateNastCredentials(NastCredentials credential, UserService userService){
        final String nastId = credential.getNastId();

        if(StringUtils.isBlank(nastId)){
            throw new BadRequestException("Expecting nastId");
        }

        User user = userService.getUserByNastId(nastId);

        if(user == null) {
            throw new NotAuthenticatedException("NastId or api key is invalid.");
        }

        if (user.isDisabled()) {
            throw new UserDisabledException(user.getNastId());
        }
    }

    public void validateUserCredentials(UserCredentials credential){
        final String username = credential.getUsername();
        final String key = credential.getKey();

        if(StringUtils.isBlank(key)){
            throw new BadRequestException("Expecting apiKey");
        }
        if(StringUtils.isBlank(username)){
            throw new BadRequestException("Expecting username");
        }
    }

    public void validatePasswordCredentials(PasswordCredentials credential, UserService userService){
        final String password = credential.getPassword();
        final String username = credential.getUsername();

        if(StringUtils.isBlank(password)){
            throw new BadRequestException("Expecting password");
        }

        if(StringUtils.isBlank(username)){
            throw new BadRequestException("Expecting username");
        }

        User user = userService.getUser(username);
        if (user == null) {
            String errMsg = "User account exists externally, but not in the AUTH database.";
            throw new NotAuthorizedException(errMsg);
        }

        if (user.isDisabled()) {
            throw new UserDisabledException("User " + username + " is not enabled.");
        }
    }

    public void validateMossoCredentials(MossoCredentials credential, UserService userService){
        final int mossoId = credential.getMossoId();
        final String key = credential.getKey();
        User user = userService.getUserByMossoId(mossoId);

        if(StringUtils.isBlank(key)){
            throw new BadRequestException("Expecting apiKey");
        }

        if (user == null) {
            throw new NotAuthenticatedException("MossoId or api key is invalid.");
        }

        if (user.isDisabled()) {
            throw new UserDisabledException(user.getMossoId().toString());
        }
    }
}
