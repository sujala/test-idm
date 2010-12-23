package com.rackspace.idm.oauthAuthentication;

public interface OauthTokenService {
    Token getToken(String tokenString);
}
