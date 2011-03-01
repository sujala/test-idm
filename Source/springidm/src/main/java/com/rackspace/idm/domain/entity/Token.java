package com.rackspace.idm.domain.entity;

import org.joda.time.DateTime;
import org.joda.time.Seconds;

public class Token {

    private String tokenString;
    private DateTime expirationTime;

    public Token() {
        // Needed by JAX-RS
    }

    public Token(String tokenString, DateTime expirationTime) {
        this.tokenString = tokenString;
        this.expirationTime = expirationTime;
    }

    public String getTokenString() {
        return tokenString;
    }

    public void setTokenString(String tokenString) {
        this.tokenString = tokenString;
    }

    public int getExpiration() {
        return secondsToExpiration(new DateTime());
    }

    public void setExpiration(int expiration) {
        expirationTime = new DateTime().plusSeconds(expiration);
    }

    public DateTime getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationTime(DateTime expirationTime) {
        this.expirationTime = expirationTime;
    }

    public boolean isExpired(DateTime current) {
        if (current == null) {
            throw new IllegalArgumentException(
                "Null value passed in for the current time");
        }
        return expirationTime.isBefore(current);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
            + ((expirationTime == null) ? 0 : expirationTime.hashCode());
        result = prime * result
            + ((tokenString == null) ? 0 : tokenString.hashCode());
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
        Token other = (Token) obj;
        if (expirationTime == null) {
            if (other.expirationTime != null) {
                return false;
            }
        } else if (!expirationTime.equals(other.expirationTime)) {
            return false;
        }
        if (tokenString == null) {
            if (other.tokenString != null) {
                return false;
            }
        } else if (!tokenString.equals(other.tokenString)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "OauthToken [expiration=" + expirationTime + ", tokenString="
            + tokenString + "]";
    }

    public int secondsToExpiration(DateTime current) {
        if (current == null) {
            throw new IllegalArgumentException(
                "Null value passed in for the time");
        }
        int seconds = Seconds.secondsBetween(current, getExpirationTime())
            .getSeconds();
        return (seconds < 0) ? 0 : seconds;
    }
}
