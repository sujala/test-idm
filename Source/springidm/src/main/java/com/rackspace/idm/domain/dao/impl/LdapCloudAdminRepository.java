package com.rackspace.idm.domain.dao.impl;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LdapCloudAdminRepository extends LdapRepository{

    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    public LdapCloudAdminRepository(LdapConnectionPools connPools, Configuration config) {
        super(connPools, config);
    }

    public boolean authenticate(String userName, String password) {

        return false;
    }

    /*
    public boolean authenticate(String userName, String password) {
        logger.debug("Authenticating cloud admin user {}", userName);
        BindResult result = null;

        Audit audit = Audit.authCloudAdmin(userName);

        String userDn = String.format("cn=%s,", userName) + CLOUD_ADMIN_BASE_DN;

        try {
            result = getBindConnPool().bind(userDn, password);
        } catch (LDAPException e) {
            if (ResultCode.INVALID_CREDENTIALS.equals(e.getResultCode())) {
                logger.info("Invalid login attempt by cloud admin {}", userName);
                audit.fail("Incorrect Credentials");
                return false;
            }
            logger.error("Bind operation on username " + userName + " failed.",
                    e);
            throw new IllegalStateException(e);
        }
        if (result == null) {
            audit.fail();
            throw new IllegalStateException("Could not get bind result.");
        }
        logger.debug(result.toString());

        audit.succeed();
        return ResultCode.SUCCESS.equals(result.getResultCode());
    }
    */
}
