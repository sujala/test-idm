package com.rackspace.idm.api.security;

import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.idm.domain.entity.Domain;
import com.rackspacecloud.docs.auth.api.v1.Credentials;
import com.rackspacecloud.docs.auth.api.v1.PasswordCredentials;
import com.rackspacecloud.docs.auth.api.v1.UserCredentials;
import lombok.Getter;
import lombok.Setter;
import org.openstack.docs.identity.api.v2.AuthenticationRequest;
import org.openstack.docs.identity.api.v2.PasswordCredentialsBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

/**
 * This class is used to hold data related to user access events and
 * is intended to simplify the storage and passing of data relevant to
 * the population of user access events.
 */
@Getter
@Setter
@Component
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class AuthenticationContext {

    private static Logger logger = LoggerFactory.getLogger(AuthenticationContext.class);

    private String username;

    private Domain domain;

    /**
     * Given an AuthenticationRequest populate the username for the request
     *
     * @param authenticationRequest
     */
    public void populateAuthRequestData(AuthenticationRequest authenticationRequest) {
        try {
            populateUsername(authenticationRequest);
        } catch(Exception e) {
            logger.warn("Unable to populate authentication context.", e);
        }
    }

    /**
     * Given an AuthenticationRequest populate the username for the request
     *
     * @param credentials
     */
    public void populateAuthRequestData(Credentials credentials) {
        if (credentials == null) return;

        try {
            if (credentials instanceof PasswordCredentials) {
                this.username = ((PasswordCredentials) credentials).getUsername();
            } else if (credentials instanceof UserCredentials) {
                this.username = ((UserCredentials) credentials).getUsername();
            }
        } catch(Exception e) {
            logger.warn("Unable to populate authentication context.", e);
        }
    }

    private void populateUsername(AuthenticationRequest authenticationRequest) {
        if (authenticationRequest == null || authenticationRequest.getCredential() == null) return;

        if (authenticationRequest.getCredential().getValue() instanceof PasswordCredentialsBase) {
            this.username = ((PasswordCredentialsBase) authenticationRequest.getCredential().getValue()).getUsername();
        } else if (authenticationRequest.getCredential().getDeclaredType().isAssignableFrom(ApiKeyCredentials.class)) {
            this.username = ((ApiKeyCredentials) authenticationRequest.getCredential().getValue()).getUsername();
        }
    }

}
