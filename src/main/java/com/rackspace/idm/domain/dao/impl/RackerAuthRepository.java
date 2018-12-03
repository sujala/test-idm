package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.ErrorCodes;
import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.RackerAuthDao;
import com.rackspace.idm.domain.dao.impl.LdapRepository.LdapSearchBuilder;
import com.rackspace.idm.exception.GatewayException;
import com.rackspace.idm.exception.NotFoundException;
import com.unboundid.ldap.sdk.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RackerAuthRepository implements RackerAuthDao {

    private static final String ATTR_MEMBERSHIP = "memberOf";

    @Autowired
    private LDAPConnectionPool connPool;

    @Autowired
    private IdentityConfig identityConfig;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public boolean authenticate(String userName, String password) {
        logger.debug("Authenticating racker {}", userName);
        Audit audit = Audit.authRacker(userName);

        String userDn;

        try {
            SearchResultEntry rackerEntry = getRackerEntry(userName);
            userDn = rackerEntry.getDN();
        } catch (NotFoundException e) {
            audit.fail("User not found in gateway");
            return false;
        } catch (GatewayException e) {
            // A gateway exception means a problem hitting AD
            audit.fail("Error searching gateway");
            throw e; // Rethrow the exception
        }

        BindResult result = null;
        try {
            result = connPool.bindAndRevertAuthentication(userDn, password);
        } catch (LDAPException e1) {
            logBindException(userName, audit, e1);
            return false;
        }

        if (result == null) {
            audit.fail("Could not get bind result.");
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
            audit.fail("Bind operation failed");
        }
    }

    @Override
    public List<String> getRackerRoles(String username) {
        SearchResultEntry entry = getRackerEntry(username);
        List<String> roles = getRackerRoles(entry);

        return roles;
    }

    private List<String> getRackerRoles(SearchResultEntry entry) {
        List<String> roles = new ArrayList<String>();
        String[] groups = entry.getAttributeValues(ATTR_MEMBERSHIP);

        if(groups != null && groups.length > 0) {
            for (String group : groups) {
                String[] split1 = group.split(",");
                roles.add(split1[0].split("=")[1]);
            }
        }
        return roles;
    }

    private SearchResultEntry getRackerEntry(String username) {
        SearchResultEntry entry = null;
        try {
            // We expect a single entry to exist in AD with the UID. Could get a size limit exceeded exception if multiple entries with same UID exist.
            final Filter searchFilter = createRackerSearchFilter(username);
            entry = getLdapInterface().searchForEntry(getBaseDn(), SearchScope.SUB, searchFilter,ATTR_MEMBERSHIP);
        } catch (LDAPException ldapEx) {
            logger.error(String.format("Encountered exception searching racker repository for user '%s'.", username), ldapEx);
            throw new GatewayException("Encountered error retrieving user.", ErrorCodes.ERROR_CODE_RACKER_PROXY_SEARCH);
        }

        if (entry == null) {
            String errMsg = String.format("Racker %s not found", username);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        return entry;
    }

    private String getBaseDn() {
        return identityConfig.getStaticConfig().getRackerAuthBaseDn();
    }

    protected LDAPInterface getLdapInterface() {
        return connPool;
    }

    private Filter createRackerSearchFilter(String username) {
        LdapSearchBuilder searchBuilder = new LdapSearchBuilder().addEqualAttribute(LdapRepository.ATTR_UID, username);

        if (identityConfig.getReloadableConfig().isFeatureOptimizeRackerSearchEnabled()) {
            searchBuilder.addEqualAttribute("objectClass", "user");
        }
        return searchBuilder.build();
    }
}
