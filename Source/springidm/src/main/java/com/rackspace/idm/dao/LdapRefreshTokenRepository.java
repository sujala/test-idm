package com.rackspace.idm.dao;

import com.rackspace.idm.entities.RefreshToken;
import com.unboundid.ldap.sdk.*;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class LdapRefreshTokenRepository extends LdapRepository implements
    RefreshTokenDao {

    public static final DateTimeFormatter DATE_PARSER = DateTimeFormat
        .forPattern("yyyyMMddHHmmss.SSS'Z");

    private static final String ATTR_OBJECT_CLASS = "objectClass";
    private static final String[] ATTR_OBJECT_CLASS_VALUES = {"top",
        "rackspaceToken"};
    private static final String ATTR_O = "o";
    private static final String ATTR_EXPIRATION = "expiration";
    private static final String ATTR_OWNER = "tokenOwner";
    private static final String ATTR_TOKEN_REQUESTOR = "tokenRequestor";

    private static final String BASE_DN = "ou=Tokens,dc=rackspace,dc=com";

    private static final String TOKEN_FIND_ALL_STRING = "(objectClass=rackspaceToken)";
    private static final String TOKEN_FIND_BY_TOKENSTRING_STRING = "(&(objectClass=rackspaceToken)(o=%s))";
    private static final String TOKEN_FIND_BY_OWNER_STRING = "(&(objectClass=rackspaceToken)(tokenOwner=%s)(expiration>=%s))";
    private static final String TOKEN_FIND_BY_OWNER_AND_REQUESTOR_STRING = "(&(objectClass=rackspaceToken)(tokenOwner=%s)(tokenRequestor=%s)(expiration>=%s))";

    public LdapRefreshTokenRepository(LdapConnectionPools connPools,
        Logger logger) {
        super(connPools, logger);
    }

    public void save(RefreshToken refreshToken) {
        getLogger().info("Saving Refresh Token - {}", refreshToken);
        if (refreshToken == null) {
            getLogger().error("Null instance of Token was passed");
            throw new IllegalArgumentException(
                "Null instance of RefreshToken was passed.");
        }

        List<Attribute> atts = new ArrayList<Attribute>();

        atts.add(new Attribute(ATTR_OBJECT_CLASS, ATTR_OBJECT_CLASS_VALUES));

        if (!StringUtils.isBlank(refreshToken.getTokenString())) {
            atts.add(new Attribute(ATTR_O, refreshToken.getTokenString()));
        }
        if (refreshToken.getExpirationTime() != null) {
            atts.add(new Attribute(ATTR_EXPIRATION, DATE_PARSER.print(refreshToken
                .getExpirationTime())));
        }
        if (!StringUtils.isBlank(refreshToken.getOwner())) {
            atts.add(new Attribute(ATTR_OWNER, refreshToken.getOwner()));
        }
        if (!StringUtils.isBlank(refreshToken.getRequestor())) {
            atts.add(new Attribute(ATTR_TOKEN_REQUESTOR, refreshToken.getRequestor()));
        }

        Attribute[] attributes = atts.toArray(new Attribute[0]);

        String tokenDN = "o=" + refreshToken.getTokenString() + "," + BASE_DN;

        LDAPResult result;
        try {
            result = getAppConnPool().add(tokenDN, attributes);
        } catch (LDAPException ldapEx) {
            getLogger().error("Error adding RefreshToken {} - {}", refreshToken, ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            getLogger().error("Error adding RefreshToken {} - {}", refreshToken,
                result.getResultCode());
            throw new IllegalArgumentException(String.format(
                "LDAP error encountered when adding RefreshToken: %s - %s", refreshToken
                    .getTokenString(), result.getResultCode().toString()));
        }

        getLogger().info("Added token - {}", refreshToken);
    }

    public void delete(String tokenString) {
        getLogger().info("Deleting refresh token - {}", tokenString);
        if (StringUtils.isBlank(tokenString)) {
            getLogger().error("Null or Empty tokenString paramenter");
            throw new IllegalArgumentException(
                "Null or Empty tokenString parameter.");
        }

        LDAPResult result = null;
        try {
            result = getAppConnPool().delete(
                getTokenDnByTokenstring(tokenString));
        } catch (LDAPException ldapEx) {
            getLogger().error("Error deleting refreshToken {} - {}", tokenString,
                ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            getLogger().error("Error deleting refreshToken {} - {}", tokenString,
                result.getResultCode());
            throw new IllegalStateException(String.format(
                "LDAP error encountered when deleting refreshToken: %s - %s"
                    + tokenString, result.getResultCode().toString()));
        }
        getLogger().info("Deleted token - {}", tokenString);
    }

    public void deleteAllTokensForUser(String username,
        Set<String> tokenRequestors) {
        if (StringUtils.isBlank(username) || tokenRequestors == null
            || tokenRequestors.isEmpty()) {
            getLogger().error("Given parameters are null or empty");
            throw new IllegalArgumentException(
                "Given parameters are null or empty");
        }
        getLogger().debug("Deleting refresh token with user {} and requestors {}", 
            username, tokenRequestors);
        int delCount = 0;
        DateTime currentTime = new DateTime();
        for (String requestor : tokenRequestors) {
            RefreshToken token = this.findTokenForOwner(username,
                requestor, currentTime);
            if (token != null) {
                this.delete(token.getTokenString());
                delCount++;
            }
        }

        getLogger().debug("{} refreshTokens were deleted for user {}", delCount, username);
    }

    public List<RefreshToken> findAll() {
        getLogger().debug("Search all refresh tokens");
        SearchResult searchResult = null;
        try {
            searchResult = getAppConnPool().search(BASE_DN, SearchScope.ONE,
                TOKEN_FIND_ALL_STRING);
        } catch (LDAPSearchException ldapEx) {
            getLogger().error(
                "Error searching for all refresh tokens under DN {} - {}", BASE_DN,
                ldapEx);
            System.out.println("Could not perform search for DN " + BASE_DN);
            throw new IllegalStateException(ldapEx);
        }

        List<RefreshToken> tokens = new ArrayList<RefreshToken>();
        for (SearchResultEntry e : searchResult.getSearchEntries()) {
            RefreshToken token = getToken(e);
            tokens.add(token);
        }

        getLogger()
            .debug("Found {} tokens under DN {}", tokens.size(), BASE_DN);
        return tokens;
    }

    public RefreshToken findByTokenString(String tokenString) {
        getLogger().debug("Searching for refreshToken {}", tokenString);
        if (StringUtils.isBlank(tokenString)) {
            getLogger().error("Null or Empty tokenString parameter");
            throw new IllegalArgumentException(
                "Null or Empty tokenString parameter.");
        }

        RefreshToken token = null;
        SearchResult searchResult = getTokenSearchResult(tokenString);

        if (searchResult.getEntryCount() == 1) {
            SearchResultEntry e = searchResult.getSearchEntries().get(0);
            token = getToken(e);
        } else if (searchResult.getEntryCount() > 1) {
            getLogger().error("More than one entry was found for refreshToken {}",
                tokenString);
            throw new IllegalStateException(
                "More than one entry was found for this refreshToken");
        }
        getLogger().debug("Found token - {}", token);

        return token;
    }

    private RefreshToken getToken(SearchResultEntry resultEntry) {
        RefreshToken token = new RefreshToken();
        token.setTokenString(resultEntry.getAttributeValue(ATTR_O));
        String expirationStr = resultEntry.getAttributeValue(ATTR_EXPIRATION);
        DateTime expiration = DATE_PARSER.parseDateTime(expirationStr);
        token.setExpirationTime(expiration);
        token.setOwner(resultEntry.getAttributeValue(ATTR_OWNER));
        token.setRequestor(resultEntry.getAttributeValue(ATTR_TOKEN_REQUESTOR));
        return token;
    }

    public RefreshToken findTokenForOwner(String owner, String requestor,
        DateTime validAfter) {
        getLogger().debug("Searching for refresh token for Owner: {}", owner);
        if (StringUtils.isBlank(owner) || StringUtils.isBlank(requestor)) {
            String error = "Null or Empty owner or requestor parameter";
            getLogger().error(error);
            throw new IllegalArgumentException(error);
        }

        RefreshToken token = null;
        SearchResult searchResult = null;
        try {
            searchResult = getAppConnPool().search(
                BASE_DN,
                SearchScope.ONE,
                String.format(TOKEN_FIND_BY_OWNER_AND_REQUESTOR_STRING, owner,
                    requestor, DATE_PARSER.print(validAfter)));
        } catch (LDAPSearchException ldapEx) {
            getLogger().error("Error searching for refreshToken for Onwer: {} - {}",
                owner, ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (searchResult.getEntryCount() == 1) {
            SearchResultEntry e = searchResult.getSearchEntries().get(0);
            token = getToken(e);
        } else if (searchResult.getEntryCount() > 1) {
            getLogger().error("More than one entry was found for refreshToken for {}",
                owner);
            throw new IllegalStateException(
                "More than one entry was found for this token");
        }
        getLogger().debug("Found refreshToken for Owner: {} - {}", owner, token);

        return token;
    }

    public String getTokenDnByTokenstring(String tokenString) {
        String dn = null;
        SearchResult searchResult = getTokenSearchResult(tokenString);
        if (searchResult.getEntryCount() == 1) {
            SearchResultEntry e = searchResult.getSearchEntries().get(0);
            dn = e.getDN();
        } else if (searchResult.getEntryCount() > 1) {
            getLogger()
                .error("More than one entry was found for tokenString {}",
                    tokenString);
            throw new IllegalStateException(
                "More than one entry was found for this tokenString");
        }
        return dn;
    }

    private SearchResult getTokenSearchResult(String tokenString) {
        SearchResult searchResult = null;
        try {
            searchResult = getAppConnPool().search(BASE_DN, SearchScope.SUB,
                String.format(TOKEN_FIND_BY_TOKENSTRING_STRING, tokenString));
        } catch (LDAPSearchException ldapEx) {
            getLogger().error("Error searching for token {} - {}", tokenString,
                ldapEx);
            throw new IllegalStateException(ldapEx);
        }
        return searchResult;
    }

    public void updateToken(RefreshToken refreshToken) { 
        
        getLogger().info("Updating refresh token {}", refreshToken);
        if (refreshToken == null) {
            getLogger().error(
                "Error updating RefreshToken: instance is null");
            throw new IllegalArgumentException(
                "Bad parameter: The RefreshToken instance is null.");
        }
        
        String tokenString = refreshToken.getTokenString();
        RefreshToken oldRefreshToken = this.findByTokenString(tokenString);

        if (oldRefreshToken == null) {
            getLogger()
                .error("No record found for refreshToken {}", tokenString);
            throw new IllegalArgumentException(
                "RefreshToken not found.");
        }

        if (refreshToken.equals(oldRefreshToken)) {
            // No changes!
            return;
        }

        LDAPResult result = null;
        try {
            result = getAppConnPool().modify(
                getTokenDnByTokenString(tokenString),
                getModifications(oldRefreshToken, refreshToken));
        } catch (LDAPException ldapEx) {
            getLogger().error("Error updating refreshToken {} - {}",
                tokenString, ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            getLogger().error("Error updating refreshToken {} - {}",
                tokenString, result.getResultCode());
            throw new IllegalArgumentException(String.format(
                "LDAP error encountered when updating refreshToken: %s - %s"
                    + tokenString, result.getResultCode().toString()));
        }

        getLogger().info("Updated refreshToken - {}", tokenString);
    }
    
    // helper funcs
    private String getTokenDnByTokenString(String tokenString) {
        String dn = null;
        
        SearchResult searchResult = null;
        try {
            searchResult = getAppConnPool().search(
                BASE_DN,
                SearchScope.SUB,
                String.format(TOKEN_FIND_BY_TOKENSTRING_STRING,
                    tokenString));
        } catch (LDAPSearchException ldapEx) {
            getLogger().error("Error searching for refreshToken {} - {}", tokenString,
                ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (searchResult.getEntryCount() == 1) {
            SearchResultEntry e = searchResult.getSearchEntries().get(0);
            dn = e.getDN();
        } else if (searchResult.getEntryCount() > 1) {
            getLogger().error("More than one entry was found for refreshToken {}",
                tokenString);
            throw new IllegalStateException(
                "More than one entry was found for this refresh token");
        }
        return dn;
    }
    
    List<Modification> getModifications(RefreshToken tOld, RefreshToken tNew) {
        List<Modification> mods = new ArrayList<Modification>();

        if (!StringUtils.equals(tOld.getOwner(), tNew.getOwner())) {
            mods.add(new Modification(ModificationType.REPLACE,
                ATTR_OWNER, tNew.getOwner()));
        }
        
        if (!StringUtils.equals(tOld.getRequestor(), tNew.getRequestor())) {
            mods.add(new Modification(ModificationType.REPLACE,
                ATTR_TOKEN_REQUESTOR, tNew.getRequestor()));
        }
        
        if (!tOld.getExpirationTime().equals(tNew.getExpirationTime())) {
            mods.add(new Modification(ModificationType.REPLACE,
                ATTR_EXPIRATION, 
                DATE_PARSER.print(tNew.getExpirationTime())));
        }
        
        return mods;
    }
}
