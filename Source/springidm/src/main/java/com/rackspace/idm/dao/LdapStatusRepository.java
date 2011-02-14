package com.rackspace.idm.dao;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;

import com.rackspace.idm.util.PingableService;
import com.unboundid.ldap.sdk.LDAPException;

public class LdapStatusRepository extends LdapRepository implements PingableService {

    public LdapStatusRepository(LdapConnectionPools conn, Configuration config, Logger logger) {
        super(conn, config, logger);
        // TODO Auto-generated constructor stub
    }
    
    
    public boolean isAlive() {    
        try {
            getAppConnPool().getHealthCheck().ensureConnectionValidForContinuedUse(getAppConnPool().getConnection());
        } catch (LDAPException e) {
           return false;
        }
        
        return true;
    }
}
