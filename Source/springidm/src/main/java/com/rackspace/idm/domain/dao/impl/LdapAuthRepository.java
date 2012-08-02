package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.dao.AuthDao;
import com.rackspace.idm.domain.dao.impl.LdapRepository.LdapSearchBuilder;
import com.rackspace.idm.exception.NotFoundException;
import com.unboundid.ldap.sdk.*;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class LdapAuthRepository implements AuthDao {

    private static final String BASE_DN = "ou=users,o=rackspace";
    private final LDAPConnectionPool connPool;
    private final Configuration config;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public LdapAuthRepository(LDAPConnectionPool connPool, Configuration config) {
        this.connPool = connPool;
        this.config = config;
    }


    @Override
    public boolean authenticate(String userName, String password) {
        logger.debug("Authenticating racker {}", userName);
        BindResult result = null;

        Audit audit = Audit.authRacker(userName);

        String userDn = String.format("cn=%s,", userName) + getBaseDn();

        try {
            result = connPool.bind(userDn, password);
        } catch (LDAPException e) {
            if (ResultCode.INVALID_CREDENTIALS.equals(e.getResultCode())) {
                logger.info("Invalid login attempt by racker {}", userName);
                audit.fail("Incorrect Credentials");
            } else {
                logger.error("Bind operation on username " + userName + " failed.", e);
                logger.error(e.getMessage());
            }
            return false;
        }
        if (result == null) {
            audit.fail();
            throw new IllegalStateException("Could not get bind result.");
        }
        logger.debug(result.toString());

        audit.succeed();
        return ResultCode.SUCCESS.equals(result.getResultCode());
    }

    @Override
    public List<String> getRackerRoles(String username) {
        List<String> roles = new ArrayList<String>();

        Filter searchFilter = new LdapSearchBuilder().addEqualAttribute(LdapRepository.ATTR_UID, username).build();

        SearchResultEntry entry = null;
        try {
            entry = getLdapInterface().searchForEntry(BASE_DN, SearchScope.ONE, searchFilter);
        } catch (LDAPSearchException ldapEx) {
            throw new IllegalStateException(ldapEx);
        }

        if (entry == null) {
            String errMsg = String.format("Racker %s not found", username);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        String[] groups = entry.getAttributeValues("groupMembership");

        for (String group : groups) {
            String[] split1 = group.split(",");
            roles.add(split1[0].split("=")[1]);
        }

        return roles;
    }

    private String getBaseDn() {
        return config.getString("auth.ldap.base.dn", BASE_DN);
    }

    protected LDAPInterface getLdapInterface() {
        return connPool;
    }
}
