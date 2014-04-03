package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.dao.AuthDao;
import com.rackspace.idm.domain.dao.impl.LdapRepository.LdapSearchBuilder;
import com.rackspace.idm.exception.NotFoundException;
import com.unboundid.ldap.sdk.*;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class LdapAuthRepository implements AuthDao {

    private static final String BASE_DN = "ou=users,o=rackspace";
    private static final String ATTR_ALIASEDOBJECTNAME = "aliasedobjectname";

    @Autowired
    private LDAPConnectionPool connPool;

    @Autowired
    private Configuration config;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public boolean authenticate(String userName, String password) {
        logger.debug("Authenticating racker {}", userName);
        Audit audit = Audit.authRacker(userName);

        BindResult result = null;
        try {
            final String userDn = String.format("cn=%s,", userName) + getBaseDn();
            result = connPool.bind(userDn, password);
        } catch (LDAPException e1) {
            if (ResultCode.INVALID_CREDENTIALS.equals(e1.getResultCode())) {
                try {
                    // Handle aliases
                    result = bindAlias(userName, password);
                } catch (LDAPException e) {
                    logBindException(userName, audit, e);
                    return false;
                }
            } else {
                logBindException(userName, audit, e1);
                return false;
            }
        }
        if (result == null) {
            audit.fail();
            throw new IllegalStateException("Could not get bind result.");
        }
        logger.debug(result.toString());

        audit.succeed();
        return ResultCode.SUCCESS.equals(result.getResultCode());
    }

    private void logBindException(String userName, Audit audit, LDAPException e) {
        if (ResultCode.INVALID_CREDENTIALS.equals(e.getResultCode())) {
            logger.info("Invalid login attempt by racker {}", userName);
            audit.fail("Incorrect Credentials");
        } else {
            logger.error("Bind operation on username " + userName + " failed.", e);
            logger.error(e.getMessage());
        }
    }

    @Override
    public List<String> getRackerRoles(String username) {
        List<String> roles = new ArrayList<String>();

        SearchResultEntry entry = null;
        try {
            final Filter searchFilter = new LdapSearchBuilder().addEqualAttribute(LdapRepository.ATTR_UID, username).build();
            entry = getLdapInterface().searchForEntry(getBaseDn(), SearchScope.ONE, searchFilter);
            if (entry == null) {
                // Handle aliases
                entry = getAliasEntry(username);
            }
        } catch (LDAPException ldapEx) {
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

    private String getAliasDN(String username) throws LDAPSearchException {
        final Filter searchFilter = new LdapSearchBuilder().addEqualAttribute(LdapRepository.ATTR_NAME, username).build();
        final SearchResultEntry entry = connPool.searchForEntry(getBaseDn(), SearchScope.ONE, DereferencePolicy.NEVER, 0, false, searchFilter, ATTR_ALIASEDOBJECTNAME);
        if (entry != null && entry.getAttribute(ATTR_ALIASEDOBJECTNAME) != null) {
            return entry.getAttribute(ATTR_ALIASEDOBJECTNAME).getValueAsDN().toNormalizedString();
        } else {
            return null;
        }
    }

    private SearchResultEntry getAliasEntry(String username) throws LDAPException {
        final String userDn = getAliasDN(username);
        if (userDn == null) {
            return null;
        } else {
            return getLdapInterface().getEntry(userDn);
        }
    }

    private BindResult bindAlias(String username, String password) throws LDAPException {
        final String realUserDn = getAliasDN(username);
        if (realUserDn == null) {
            throw new LDAPException(ResultCode.INVALID_CREDENTIALS);
        } else {
            return connPool.bind(realUserDn, password);
        }
    }

}
