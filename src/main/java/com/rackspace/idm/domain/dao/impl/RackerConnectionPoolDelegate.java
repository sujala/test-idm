package com.rackspace.idm.domain.dao.impl;

import com.unboundid.ldap.sdk.*;

/**
 * Creating this wrapper to make mocking easier as LDapConnectionPool is a final class which Spock does not directly support
 * creating mocks against. Using PockerMock/PowerMockito with spock testing is a convoluted nightmare. So creating this
 * class to allow for easier testing and to better distinguish between the pool used for AD (Racker) vs Customer Identity (CA).
 *
 * This class needs to remain just a delegate -
 * otherwise will create the exact same problems trying to test this class which was the reason for creating this class
 * in the first place.
 *
 * Note - can't use Lombok's @Delegate experimental annotation due to issues generating methods that contain varargs.
 * We only use a couple of the methods anyway so easy enough to explicitly define them.
 */
public class RackerConnectionPoolDelegate {
    private LDAPConnectionPool rackerConnectionPool;

    public RackerConnectionPoolDelegate(LDAPConnectionPool rackerConnectionPool) {
        this.rackerConnectionPool = rackerConnectionPool;
    }

    public BindResult bindAndRevertAuthentication(String bindDN, String password, Control... controls) throws LDAPException {
        return rackerConnectionPool.bindAndRevertAuthentication(bindDN, password, controls);
    }

    public SearchResultEntry searchForEntry(String baseDN, SearchScope scope, Filter filter, String... attributes) throws LDAPSearchException {
        return rackerConnectionPool.searchForEntry(baseDN, scope, filter, attributes);
    }

    public void close() {
        rackerConnectionPool.close();
    }
}
