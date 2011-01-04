package com.rackspace.idm.oauthAuthentication;

public class AuthResult {

    protected int httpStatusCode;
    protected String message;

    public AuthResult() {
    }

    public void setHttpStatusCode(int httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + httpStatusCode;
        result = prime * result + ((message == null) ? 0 : message.hashCode());
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
        AuthResult other = (AuthResult) obj;
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
}
