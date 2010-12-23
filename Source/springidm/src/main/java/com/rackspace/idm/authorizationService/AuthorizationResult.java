package com.rackspace.idm.authorizationService;

import com.rackspace.idm.oauthAuthentication.AuthResult;

public class AuthorizationResult extends AuthResult {

    boolean result;

    public AuthorizationResult() {

    }

    public AuthorizationResult(boolean result) {
        this.result = result;
    }

    public AuthorizationResult(boolean result, String message) {
        this.result = result;
        this.message = message;
    }

    public boolean getResultValue() {
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AuthorizationResult other = (AuthorizationResult) obj;
        if (httpStatusCode != other.httpStatusCode) {
            return false;
        }
        if (message == null) {
            if (other.message != null) {
                return false;
            }
        } else if (!message.equals(other.message)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "AuthorizationResult [httpStatusCode=" + httpStatusCode
            + ", message=" + message + "]";
    }

}
