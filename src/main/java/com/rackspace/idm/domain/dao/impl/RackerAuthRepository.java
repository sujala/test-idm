package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.ErrorCodes;
import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.config.CacheConfiguration;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.RackerAuthDao;
import com.rackspace.idm.domain.dao.impl.LdapRepository.LdapSearchBuilder;
import com.rackspace.idm.exception.GatewayException;
import com.rackspace.idm.exception.NotFoundException;
import com.unboundid.ldap.sdk.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RackerAuthRepository implements RackerAuthDao {

    public static final String ATTR_MEMBERSHIP = "memberOf";

    @Autowired
    private RackerConnectionPoolDelegate rackerConnectionPool;

    @Autowired
    private IdentityConfig identityConfig;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Only want to cache successful results at this time,
     *
     * For other cases, we don't want to cache - even for invalid credentials. We need more analysis to distinguish between
     * an unsuccessful auth due to invalid credentials and unsuccessful auth due to the account being locked out. We
     * don't necessarily want to extend the lockout period in identity. More discussion needs to happen around this to
     * determine whether we should cache failures due to lockout (which could potentially extend the lockout period due
     * to identity cache by the TTL of the cache). Other strategies would need to be developed to deal with other types
     * of failures (e.g. AD downtime, timeouts, etc).
     *
     * @param userName
     * @param password
     * @return
     */
    @Override
    @Cacheable(value = CacheConfiguration.RACKER_AUTH_RESULT_CACHE, keyGenerator = "hashedStringKeyGenerator", unless="#result != T(com.rackspace.idm.domain.dao.impl.RackerAuthResult).SUCCESS")
    public RackerAuthResult authenticateWithCache(String userName, String password) {
        logger.debug(String.format("Missed auth result cache for username '%s'", userName));
        RackerAuthResult result = authenticateInternal(userName, password);
        return result;
    }

    @Override
    public boolean authenticate(String userName, String password) {
        RackerAuthResult result = authenticateInternal(userName, password);
        return result == RackerAuthResult.SUCCESS;
    }

    private RackerAuthResult authenticateInternal(String userName, String password) {
        logger.debug("Authenticating racker {}", userName);
        Audit audit = Audit.authRacker(userName);

        String userDn;

        try {
            SearchResultEntry rackerEntry = getRackerEntry(userName);
            userDn = rackerEntry.getDN();
        } catch (NotFoundException e) {
            audit.fail("User not found in gateway");
            return RackerAuthResult.USER_NOT_FOUND;
        } catch (GatewayException e) {
            // A gateway exception means a problem hitting AD
            audit.fail("Error searching gateway");
            throw e; // Rethrow the exception
        }

        BindResult result = null;
        try {
            result = rackerConnectionPool.bindAndRevertAuthentication(userDn, password);
        } catch (LDAPException e1) {
            // Legacy code assumes an LDAPException is thrown when auth fails due to invalid credentials.
            logBindException(userName, audit, e1);
            if (ResultCode.INVALID_CREDENTIALS.equals(e1.getResultCode())) {
                return RackerAuthResult.INVALID_CREDENTIALS;
            } else {
                return RackerAuthResult.UNKNOWN_FAILURE;
            }
        }

        if (result == null) {
            audit.fail("Could not get bind result.");
            throw new IllegalStateException("Could not get bind result.");
        }
        logger.debug(result.toString());

        if (ResultCode.SUCCESS.equals(result.getResultCode())) {
            audit.succeed();
            return RackerAuthResult.SUCCESS;
        } else {
            audit.fail(String.format("Unknown failure '%s'. Auth was not successful", result.getResultCode()));
            return RackerAuthResult.UNKNOWN_FAILURE;
        }
    }

    private void logBindException(String userName, Audit audit, LDAPException e) {
        if (ResultCode.INVALID_CREDENTIALS.equals(e.getResultCode())) {
            logger.info("Invalid login attempt by racker {}", userName);
            audit.fail("Incorrect Credentials");
        } else {
            logger.error("Bind operation on username " + userName + " failed.", e);
            audit.fail("Bind operation failed");
        }
    }

    @Override
    @Cacheable(value = CacheConfiguration.RACKER_GROUPS_CACHE, unless="#result == null")
    public List<String> getRackerRolesWithCache(String userName) {
        logger.debug(String.format("Missed racker group cache for username '%s'", userName));
        List<String> roles = getRackerRoles(userName);
        return roles;
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
            entry = rackerConnectionPool.searchForEntry(getBaseDn(), SearchScope.SUB, searchFilter,ATTR_MEMBERSHIP);
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

    private Filter createRackerSearchFilter(String username) {
        LdapSearchBuilder searchBuilder = new LdapSearchBuilder().addEqualAttribute(LdapRepository.ATTR_UID, username);

        if (identityConfig.getReloadableConfig().isFeatureOptimizeRackerSearchEnabled()) {
            searchBuilder.addEqualAttribute("objectClass", "user");
        }
        return searchBuilder.build();
    }
}
