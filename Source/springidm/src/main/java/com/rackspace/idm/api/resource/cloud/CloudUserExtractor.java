package com.rackspace.idm.api.resource.cloud;

import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.UserService;
import com.rackspacecloud.docs.auth.api.v1.Credentials;
import com.rackspacecloud.docs.auth.api.v1.MossoCredentials;
import com.rackspacecloud.docs.auth.api.v1.NastCredentials;
import com.rackspacecloud.docs.auth.api.v1.UserCredentials;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: matt.colton
 * Date: 12/28/11
 * Time: 5:33 PM
 * To change this template use File | Settings | File Templates.
 */

@Component
public class CloudUserExtractor {

    @Autowired
    private CloudExceptionResponse cloudExceptionResponse;
    
    @Autowired
    private UserService userService;

    public CloudUserExtractor(){}
    
    public CloudUserExtractor(CloudExceptionResponse cloudExceptionResponse, UserService userService) {
        this.cloudExceptionResponse = cloudExceptionResponse;
        this.userService = userService;
    }

    public User getUserByCredentialType(JAXBElement<? extends Credentials> credentials) throws IOException {
        String username;
        User user = null;
        if (credentials.getValue() instanceof UserCredentials) {
            UserCredentials userCreds = (UserCredentials) credentials.getValue();
            username = userCreds.getUsername();
            String apiKey = userCreds.getKey();
            if (apiKey == null || apiKey.length() == 0) {
                throw new CloudExceptionResponse(cloudExceptionResponse.badRequestExceptionResponse("Expecting apiKey"));
            }
            if (username == null || username.length() == 0) {
                throw new CloudExceptionResponse(cloudExceptionResponse.badRequestExceptionResponse("Expecting username"));
            }
            user = userService.getUser(username);
        } else if (credentials.getValue() instanceof com.rackspacecloud.docs.auth.api.v1.PasswordCredentials) {
            username = ((com.rackspacecloud.docs.auth.api.v1.PasswordCredentials) credentials.getValue()).getUsername();
            String password = ((com.rackspacecloud.docs.auth.api.v1.PasswordCredentials) credentials.getValue()).getPassword();
            if (password == null || password.length() == 0) {
                throw new CloudExceptionResponse(cloudExceptionResponse.badRequestExceptionResponse("Expecting password"));
            }
            if (username == null || username.length() == 0) {
                throw new CloudExceptionResponse(cloudExceptionResponse.badRequestExceptionResponse("Expecting username"));
            }
            user = userService.getUser(username);
        } else if (credentials.getValue() instanceof MossoCredentials) {
            Integer mossoId = ((MossoCredentials) credentials.getValue()).getMossoId();
            String key = ((MossoCredentials) credentials.getValue()).getKey();
            if (mossoId == null) {
                throw new CloudExceptionResponse(cloudExceptionResponse.badRequestExceptionResponse("Expecting mosso id"));
            }
            user = userService.getUserByMossoId(mossoId);
        } else if (credentials.getValue() instanceof NastCredentials) {
            String nastId = ((NastCredentials) credentials.getValue()).getNastId();
            String key = ((NastCredentials) credentials.getValue()).getKey();
            if (nastId == null || nastId.length() == 0) {
                throw new CloudExceptionResponse(cloudExceptionResponse.badRequestExceptionResponse("Expecting nast id"));
            }
            user = userService.getUserByNastId(nastId);
        }
        return user;
    }
}
