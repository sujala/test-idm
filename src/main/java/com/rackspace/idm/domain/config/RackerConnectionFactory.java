package com.rackspace.idm.domain.config;

import com.unboundid.ldap.sdk.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLSocketFactory;

/**
 * A connection factory for connections to eDir. This class
 * is intended to make the code creating connections to eDir
 * more readable and easier to test.
 */
@Component
public class RackerConnectionFactory {

    @Autowired
    IdentityConfig identityConfig;

    public LDAPConnection createAuthenticatedEncryptedConnection(SSLSocketFactory socketFactory, String host, int port, String bindDn, String bindPw) throws LDAPException {
        return new LDAPConnection(socketFactory, host, port, bindDn, bindPw);
    }

    public LDAPConnectionPool createConnectionPool(LDAPConnection conn, int initPoolSize, int maxPoolSize) throws LDAPException {
        return new LDAPConnectionPool(conn, initPoolSize, maxPoolSize);
    }
}
