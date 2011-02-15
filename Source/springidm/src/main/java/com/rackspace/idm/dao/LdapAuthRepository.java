package com.rackspace.idm.dao;

import org.slf4j.Logger;

import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.ExtendedResult;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPSearchException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldap.sdk.extensions.StartTLSExtendedRequest;

public class LdapAuthRepository implements AuthDao {
    private Logger logger;
    private static final String USER_FIND_BY_USERNAME = "(&(objectClass=inetOrgPerson)(cn=%s))";
    private static final String BASE_DN = "ou=users,o=rackspace";
    private LDAPConnectionPool connPool;

    public LdapAuthRepository(LDAPConnectionPool connPool, Logger logger) {
        this.connPool = connPool;
        this.logger = logger;
    }

    public boolean authenticate(String userName, String password) {
        logger.debug("Authenticating user {}", userName);
        BindResult result = null;
        LDAPConnection conn = null;
        
        String userDn = getUserDnByUsername(userName);
        
        if (userDn == null) {
            return false;
        }
        try {
            result = connPool.bind(userDn, password);
        } catch (LDAPException e) {
            if (ResultCode.INVALID_CREDENTIALS.equals(e.getResultCode())) {
                logger.info(
                    "Invalid login attempt by user {} with password {}.",
                    userName, password);
                return false;
            }
            logger.error("Bind operation on username " + userName + " failed.",
                e);
            throw new IllegalStateException(e);
        } finally {
            if (conn != null) {
                conn.close();
            }
            logger.debug("conn count: {}", connPool
                .getCurrentAvailableConnections());
        }
        if (result == null) {
            throw new IllegalStateException("Could not get bind result.");
        }
        logger.debug(result.toString());
        return ResultCode.SUCCESS.equals(result.getResultCode());
    }

    private String getUserDnByUsername(String username) {
        String dn = null;
        SearchResult searchResult = getUserSearchResult(username);
        if (searchResult.getEntryCount() == 1) {
            SearchResultEntry e = searchResult.getSearchEntries().get(0);
            dn = e.getDN();
        } else if (searchResult.getEntryCount() > 1) {
            logger.error("More than one entry was found for username {}",
                username);
            throw new IllegalStateException(
                "More than one entry was found for this username");
        }
        return dn;
    }

    private SearchResult getUserSearchResult(String username) {
        SearchResult searchResult = null;
        try {
            searchResult = connPool.search(BASE_DN, SearchScope.SUB, String
                .format(USER_FIND_BY_USERNAME, username));
        } catch (LDAPSearchException ldapEx) {
            logger.error("Error searching for username {} - {}", username,
                ldapEx);
            throw new IllegalStateException(ldapEx);
        }
        return searchResult;
    }
}
