package com.rackspace.idm.domain.dao.impl;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.Seconds;

import com.rackspace.idm.domain.entity.Auditable;

public class UserTokenStrings implements Serializable, Auditable {
    private static final long serialVersionUID = 7761050825003817823L;

    private String ownerId;
    private Map<String, String> lookupByRequestor = new HashMap<String, String>();
    private DateTime expirationTime;

    UserTokenStrings(String ownerId) {
        this.ownerId = ownerId;
    }

    String get(String requestorId) {
        return lookupByRequestor.get(requestorId);
    }

    void put(String requestorId, int expiration, String tokenString) {
        lookupByRequestor.put(requestorId, tokenString);
        DateTime tokenExp = new DateTime().plusSeconds(expiration);
        if (tokenExp.isAfter(expirationTime)) {
            expirationTime = tokenExp;
        }
    }

    void remove(String requestorId) {
        lookupByRequestor.remove(requestorId);
    }

    String getOwnerId() {
        return ownerId;
    }

    int getExpiration(DateTime currentDateTime) {
        if (expirationTime == null) {
            return 0;
        }
        int seconds = Seconds.secondsBetween(currentDateTime, expirationTime).getSeconds();
        return (seconds < 0) ? 0 : seconds;
    }

    List<String> getTokenStrings() {
        Collection<String> tokenStrs = lookupByRequestor.values();
        return Collections.unmodifiableList(new ArrayList<String>(tokenStrs));
    }

    @Override
    public String getAuditContext() {
        return String.format("Owner: %s, expiration: %s, tokens: %s", ownerId, expirationTime,
            lookupByRequestor);
    }

    /**
     * Used by Java serialization. Produces the serialized form of the
     * class.
     *
     * @return The proxy instance of the Token class
     */
    private Object writeReplace() {
        return new SerializationProxy(this);
    }

    /**
     * Used by Java serialization. Prevent attempts to deserialize the
     * object directly, without using the proxy object.
     *
     * @param stream Used by Java serialization API
     * @throws java.io.InvalidObjectException By the Java serialization API
     */
    private void readObject(ObjectInputStream stream) throws InvalidObjectException {
        throw new InvalidObjectException("Serialization proxy is required.");
    }

    /**
     * Serialized form for the object, based on the Serialization Proxy
     * pattern in the book Effective Java, 2nd Edition, p. 312
     * <p/>
     * I.e., this is what actually gets serialized.
     */
    private static class SerializationProxy implements Serializable {
        private static final long serialVersionUID = 940036102219521577L;

        private String ownerId;
        private Map<String, String> lookupByRequestor;
        private DateTime expiration;

        SerializationProxy(UserTokenStrings tokenStrings) {
            this.ownerId = tokenStrings.ownerId;
            this.lookupByRequestor = tokenStrings.lookupByRequestor;
            this.expiration = tokenStrings.expirationTime;
        }

        private Object readResolve() {
            UserTokenStrings tokenStrings = new UserTokenStrings(ownerId);
            tokenStrings.lookupByRequestor = lookupByRequestor;
            tokenStrings.expirationTime = expiration;

            return tokenStrings;
        }

    }
}
