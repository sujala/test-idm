package com.rackspace.idm.api.resource.cloud;

import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserScopeAccess;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspacecloud.docs.auth.api.v1.Credentials;
import com.rackspacecloud.docs.auth.api.v1.MossoCredentials;
import com.rackspacecloud.docs.auth.api.v1.NastCredentials;
import com.rackspacecloud.docs.auth.api.v1.UserCredentials;
import org.apache.commons.lang.StringUtils;
import org.openstack.docs.identity.api.v2.AuthenticationRequest;
import org.openstack.docs.identity.api.v2.PasswordCredentialsRequiredUsername;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;

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

    @Autowired
    private ScopeAccessService scopeAccessService;

    public CloudUserExtractor(){}
    
    public CloudUserExtractor(CloudExceptionResponse cloudExceptionResponse, UserService userService, ScopeAccessService scopeAccessService) {
        this.cloudExceptionResponse = cloudExceptionResponse;
        this.userService = userService;
        this.scopeAccessService = scopeAccessService;
    }

    public User getUserByV20CredentialType(AuthenticationRequest authenticationRequest){
        try {
            User user = null;
            UserScopeAccess usa = null;
            if (authenticationRequest.getToken() != null && !StringUtils.isBlank(authenticationRequest.getToken().getId())) {
                usa = (UserScopeAccess) scopeAccessService.getScopeAccessByAccessToken(authenticationRequest.getToken().getId());
                user = userService.getUser(usa.getUsername());
            } else if (authenticationRequest.getCredential().getDeclaredType().isAssignableFrom(PasswordCredentialsRequiredUsername.class)) {
                PasswordCredentialsRequiredUsername creds = (PasswordCredentialsRequiredUsername) authenticationRequest.getCredential().getValue();
                user = userService.getUser(creds.getUsername());
            } else if (authenticationRequest.getCredential().getDeclaredType().isAssignableFrom(ApiKeyCredentials.class)) {
                ApiKeyCredentials creds = (ApiKeyCredentials) authenticationRequest.getCredential().getValue();
                user = userService.getUser(creds.getUsername());
            }
            return user;
        } catch (Exception ex) {
            return null;
        }
    }

    public User getUserByCredentialType(JAXBElement<? extends Credentials> credentials) {
        String username;
        User user = null;
        if (credentials.getValue() instanceof UserCredentials) {
            UserCredentials userCreds = (UserCredentials) credentials.getValue();
            username = userCreds.getUsername();
            String apiKey = userCreds.getKey();
            if (StringUtils.isBlank(apiKey)) {
                throw new CloudExceptionResponse(cloudExceptionResponse.badRequestExceptionResponse("Expecting apiKey"));
            }
            if (StringUtils.isBlank(username)) {
                throw new CloudExceptionResponse(cloudExceptionResponse.badRequestExceptionResponse("Expecting username"));
            }
            user = userService.getUser(username);
        } else if (credentials.getValue() instanceof com.rackspacecloud.docs.auth.api.v1.PasswordCredentials) {
            username = ((com.rackspacecloud.docs.auth.api.v1.PasswordCredentials) credentials.getValue()).getUsername();
            String password = ((com.rackspacecloud.docs.auth.api.v1.PasswordCredentials) credentials.getValue()).getPassword();
            if (StringUtils.isBlank(password)) {
                throw new CloudExceptionResponse(cloudExceptionResponse.badRequestExceptionResponse("Expecting password"));
            }
            if (StringUtils.isBlank(username)) {
                throw new CloudExceptionResponse(cloudExceptionResponse.badRequestExceptionResponse("Expecting username"));
            }
            user = userService.getUser(username);
        } else if (credentials.getValue() instanceof MossoCredentials) {
            Integer mossoId = ((MossoCredentials) credentials.getValue()).getMossoId();
            String key = ((MossoCredentials) credentials.getValue()).getKey();
            if (key == null) {
                throw new CloudExceptionResponse(cloudExceptionResponse.badRequestExceptionResponse("Expecting mosso key"));
            }
            user = userService.getUserByMossoId(mossoId);
        } else if (credentials.getValue() instanceof NastCredentials) {
            String nastId = ((NastCredentials) credentials.getValue()).getNastId();
            if (StringUtils.isBlank(nastId)) {
                throw new CloudExceptionResponse(cloudExceptionResponse.badRequestExceptionResponse("Expecting nast id"));
            }
            user = userService.getUserByNastId(nastId);
        }
        return user;
    }
}
