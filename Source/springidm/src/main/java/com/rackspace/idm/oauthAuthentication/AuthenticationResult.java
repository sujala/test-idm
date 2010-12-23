package com.rackspace.idm.oauthAuthentication;

public class AuthenticationResult extends AuthResult {

    public AuthenticationResult() {

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
        AuthenticationResult other = (AuthenticationResult) obj;
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
        return "AuthenticationResult [httpStatusCode=" + httpStatusCode
            + ", message=" + message + "]";
    }
}
