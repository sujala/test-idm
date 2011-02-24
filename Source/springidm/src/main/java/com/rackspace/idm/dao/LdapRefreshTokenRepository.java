package com.rackspace.idm.dao;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.entities.RefreshToken;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPResult;
import com.unboundid.ldap.sdk.LDAPSearchException;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;

public class LdapRefreshTokenRepository extends LdapRepository implements RefreshTokenDao {

    public LdapRefreshTokenRepository(LdapConnectionPools connPools, Configuration config) {
        super(connPools, config);
    }

    @Override
    public void delete(String tokenString) {
        getLogger().info("Deleting refresh token - {}", tokenString);
        if (StringUtils.isBlank(tokenString)) {
            getLogger().error("Null or Empty tokenString paramenter");
            throw new IllegalArgumentException("Null or Empty tokenString parameter.");
        }

        Audit audit = Audit.log(tokenString).delete();
        LDAPResult result = null;
        try {
            result = getAppConnPool().delete(getTokenDnByTokenstring(tokenString));
        } catch (LDAPException ldapEx) {
            audit.fail();
            getLogger().error("Error deleting refreshToken {} - {}", tokenString, ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            audit.fail();
            getLogger().error("Error deleting refreshToken {} - {}", tokenString, result.getResultCode());
            throw new IllegalStateException(String.format(
                "LDAP error encountered when deleting refreshToken: %s - %s" + tokenString, result
                    .getResultCode().toString()));
        }

        audit.succeed();
        getLogger().info("Deleted token - {}", tokenString);
    }

    public void deleteAllTokensForUser(String username) {
        if (StringUtils.isBlank(username)) {
            getLogger().error("Username cannot be blank.");
            throw new IllegalArgumentException("Username cannot be blank.");
        }
        getLogger().debug("Deleting all refresh tokens for user {}", username);
        int delCount = 0;

        String searchFilter = new LdapSearchBuilder().addEqualAttribute(ATTR_TOKEN_OWNER, username)
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACETOKEN).build();

        SearchResult searchResult = null;
        try {
            searchResult = getAppConnPool().search(TOKEN_BASE_DN, SearchScope.ONE, searchFilter);
        } catch (LDAPSearchException ldapEx) {
            getLogger().error("Error searching for refreshToken for Owner: {} - {}", username, ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (searchResult.getEntryCount() > 0) {
            for (SearchResultEntry entry : searchResult.getSearchEntries()) {
                RefreshToken rToken = this.getToken(entry);
                if (rToken != null) {
                    this.delete(rToken.getTokenString());
                    delCount++;
                }
            }
        }

        getLogger().debug("{} refreshTokens were deleted for user {}", delCount, username);
    }

    public void deleteTokenForUserByClientId(String username, String clientId) {
        if (StringUtils.isBlank(username) || StringUtils.isBlank(clientId)) {
            getLogger().error("Username and ClientId cannot be blank.");
            throw new IllegalArgumentException("Username and ClientId cannot be blank.");
        }
        getLogger().debug("Deleting all refresh tokens for user {} and client {}", username, clientId);
        int delCount = 0;

        String searchFilter = new LdapSearchBuilder().addEqualAttribute(ATTR_TOKEN_OWNER, username)
            .addEqualAttribute(ATTR_TOKEN_REQUESTOR, clientId)
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACETOKEN).build();

        SearchResult searchResult = null;
        try {
            searchResult = getAppConnPool().search(TOKEN_BASE_DN, SearchScope.ONE, searchFilter);
        } catch (LDAPSearchException ldapEx) {
            getLogger().error("Error searching for refreshToken for Owner: {} - {}", username, ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (searchResult.getEntryCount() > 0) {
            for (SearchResultEntry entry : searchResult.getSearchEntries()) {
                RefreshToken rToken = this.getToken(entry);
                if (rToken != null) {
                    this.delete(rToken.getTokenString());
                    delCount++;
                }
            }
        }

        getLogger().debug("{} refreshTokens were deleted for user {}", delCount, username);
    }

    public List<RefreshToken> findAll() {
        getLogger().debug("Search all refresh tokens");
        SearchResult searchResult = null;

        String searchFilter = new LdapSearchBuilder().addEqualAttribute(ATTR_OBJECT_CLASS,
            OBJECTCLASS_RACKSPACETOKEN).build();

        try {
            searchResult = getAppConnPool().search(TOKEN_BASE_DN, SearchScope.ONE, searchFilter);
        } catch (LDAPSearchException ldapEx) {
            getLogger().error("Error searching for all refresh tokens under DN {} - {}", TOKEN_BASE_DN,
                ldapEx);
            System.out.println("Could not perform search for DN " + TOKEN_BASE_DN);
            throw new IllegalStateException(ldapEx);
        }

        List<RefreshToken> tokens = new ArrayList<RefreshToken>();
        for (SearchResultEntry e : searchResult.getSearchEntries()) {
            RefreshToken token = getToken(e);
            tokens.add(token);
        }

        getLogger().debug("Found {} tokens under DN {}", tokens.size(), TOKEN_BASE_DN);
        return tokens;
    }

    public RefreshToken findByTokenString(String tokenString) {
        getLogger().debug("Searching for refreshToken {}", tokenString);
        if (StringUtils.isBlank(tokenString)) {
            getLogger().error("Null or Empty tokenString parameter");
            throw new IllegalArgumentException("Null or Empty tokenString parameter.");
        }

        RefreshToken token = null;
        SearchResult searchResult = getTokenSearchResult(tokenString);

        if (searchResult.getEntryCount() == 1) {
            SearchResultEntry e = searchResult.getSearchEntries().get(0);
            token = getToken(e);
        } else if (searchResult.getEntryCount() > 1) {
            getLogger().error("More than one entry was found for refreshToken {}", tokenString);
            throw new IllegalStateException("More than one entry was found for this refreshToken");
        }
        getLogger().debug("Found token - {}", token);

        return token;
    }

    public RefreshToken findTokenForOwner(String owner, String requestor, DateTime validAfter) {
        getLogger().debug("Searching for refresh token for Owner: {}", owner);
        if (StringUtils.isBlank(owner) || StringUtils.isBlank(requestor)) {
            String error = "Null or Empty owner or requestor parameter";
            getLogger().error(error);
            throw new IllegalArgumentException(error);
        }

        RefreshToken token = null;
        SearchResult searchResult = null;

        String searchFilter = new LdapSearchBuilder().addEqualAttribute(ATTR_TOKEN_OWNER, owner)
            .addEqualAttribute(ATTR_TOKEN_REQUESTOR, requestor)
            .addGreaterOrEqualAttribute(ATTR_EXPIRATION, DATE_PARSER.print(validAfter))
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACETOKEN).build();

        try {
            searchResult = getAppConnPool().search(TOKEN_BASE_DN, SearchScope.ONE, searchFilter);
        } catch (LDAPSearchException ldapEx) {
            getLogger().error("Error searching for refreshToken for Onwer: {} - {}", owner, ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (searchResult.getEntryCount() > 0) {
            for (SearchResultEntry entry : searchResult.getSearchEntries()) {
                RefreshToken rToken = this.getToken(entry);
                if (token == null || rToken.getExpirationTime().isAfter(token.getExpirationTime())) {
                    token = rToken;
                }
            }
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
            getLogger().error("More than one entry was found for tokenString {}", tokenString);
            throw new IllegalStateException("More than one entry was found for this tokenString");
        }
        return dn;
    }

    public void save(RefreshToken refreshToken) {
        getLogger().info("Saving Refresh Token - {}", refreshToken);
        if (refreshToken == null) {
            getLogger().error("Null instance of Token was passed");
            throw new IllegalArgumentException("Null instance of RefreshToken was passed.");
        }

        List<Attribute> atts = new ArrayList<Attribute>();

        atts.add(new Attribute(ATTR_OBJECT_CLASS, ATTR_TOKEN_OBJECT_CLASS_VALUES));

        if (!StringUtils.isBlank(refreshToken.getTokenString())) {
            atts.add(new Attribute(ATTR_O, refreshToken.getTokenString()));
        }
        if (refreshToken.getExpirationTime() != null) {
            atts.add(new Attribute(ATTR_EXPIRATION, DATE_PARSER.print(refreshToken.getExpirationTime())));
        }
        if (!StringUtils.isBlank(refreshToken.getOwner())) {
            atts.add(new Attribute(ATTR_TOKEN_OWNER, refreshToken.getOwner()));
        }
        if (!StringUtils.isBlank(refreshToken.getRequestor())) {
            atts.add(new Attribute(ATTR_TOKEN_REQUESTOR, refreshToken.getRequestor()));
        }

        Attribute[] attributes = atts.toArray(new Attribute[0]);

        String tokenDN = new LdapDnBuilder(TOKEN_BASE_DN).addAttriubte(ATTR_O, refreshToken.getTokenString())
            .build();

        Audit audit = Audit.log(refreshToken).add();
        LDAPResult result;
        try {
            result = getAppConnPool().add(tokenDN, attributes);
        } catch (LDAPException ldapEx) {
            audit.fail();
            getLogger().error("Error adding RefreshToken {} - {}", refreshToken, ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            audit.fail();
            getLogger().error("Error adding RefreshToken {} - {}", refreshToken, result.getResultCode());
            throw new IllegalArgumentException(String.format(
                "LDAP error encountered when adding RefreshToken: %s - %s", refreshToken.getTokenString(),
                result.getResultCode().toString()));
        }

        audit.succeed();
        getLogger().info("Added token - {}", refreshToken);
    }

    public void updateToken(RefreshToken refreshToken) {

        getLogger().info("Updating refresh token {}", refreshToken);
        if (refreshToken == null) {
            getLogger().error("Error updating RefreshToken: instance is null");
            throw new IllegalArgumentException("Bad parameter: The RefreshToken instance is null.");
        }

        String tokenString = refreshToken.getTokenString();
        RefreshToken oldRefreshToken = this.findByTokenString(tokenString);

        if (oldRefreshToken == null) {
            getLogger().error("No record found for refreshToken {}", tokenString);
            throw new IllegalArgumentException("RefreshToken not found.");
        }

        if (refreshToken.equals(oldRefreshToken)) {
            // No changes!
            return;
        }

        Audit audit = Audit.log(refreshToken).modify();
        LDAPResult result = null;
        try {
            result = getAppConnPool().modify(getTokenDnByTokenString(tokenString),
                getModifications(oldRefreshToken, refreshToken));
        } catch (LDAPException ldapEx) {
            audit.fail();
            getLogger().error("Error updating refreshToken {} - {}", tokenString, ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            audit.fail();
            getLogger().error("Error updating refreshToken {} - {}", tokenString, result.getResultCode());
            throw new IllegalArgumentException(String.format(
                "LDAP error encountered when updating refreshToken: %s - %s" + tokenString, result
                    .getResultCode().toString()));
        }

        audit.succeed();
        getLogger().info("Updated refreshToken - {}", tokenString);
    }

    private RefreshToken getToken(SearchResultEntry resultEntry) {
        RefreshToken token = new RefreshToken();
        token.setTokenString(resultEntry.getAttributeValue(ATTR_O));
        String expirationStr = resultEntry.getAttributeValue(ATTR_EXPIRATION);
        DateTime expiration = DATE_PARSER.parseDateTime(expirationStr);
        token.setExpirationTime(expiration);
        token.setOwner(resultEntry.getAttributeValue(ATTR_TOKEN_OWNER));
        token.setRequestor(resultEntry.getAttributeValue(ATTR_TOKEN_REQUESTOR));
        return token;
    }

    // helper funcs
    private String getTokenDnByTokenString(String tokenString) {
        String dn = null;

        String searchFilter = new LdapSearchBuilder().addEqualAttribute(ATTR_O, tokenString)
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACETOKEN).build();

        SearchResult searchResult = null;
        try {
            searchResult = getAppConnPool().search(TOKEN_BASE_DN, SearchScope.SUB, searchFilter);
        } catch (LDAPSearchException ldapEx) {
            getLogger().error("Error searching for refreshToken {} - {}", tokenString, ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (searchResult.getEntryCount() == 1) {
            SearchResultEntry e = searchResult.getSearchEntries().get(0);
            dn = e.getDN();
        } else if (searchResult.getEntryCount() > 1) {
            getLogger().error("More than one entry was found for refreshToken {}", tokenString);
            throw new IllegalStateException("More than one entry was found for this refresh token");
        }
        return dn;
    }

    private SearchResult getTokenSearchResult(String tokenString) {
        SearchResult searchResult = null;

        String searchFilter = new LdapSearchBuilder().addEqualAttribute(ATTR_O, tokenString)
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACETOKEN).build();

        try {
            searchResult = getAppConnPool().search(TOKEN_BASE_DN, SearchScope.SUB, searchFilter);
        } catch (LDAPSearchException ldapEx) {
            getLogger().error("Error searching for token {} - {}", tokenString, ldapEx);
            throw new IllegalStateException(ldapEx);
        }
        return searchResult;
    }

    List<Modification> getModifications(RefreshToken tOld, RefreshToken tNew) {
        List<Modification> mods = new ArrayList<Modification>();

        if (!StringUtils.equals(tOld.getOwner(), tNew.getOwner())) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_TOKEN_OWNER, tNew.getOwner()));
        }

        if (!StringUtils.equals(tOld.getRequestor(), tNew.getRequestor())) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_TOKEN_REQUESTOR, tNew.getRequestor()));
        }

        if (!tOld.getExpirationTime().equals(tNew.getExpirationTime())) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_EXPIRATION, DATE_PARSER.print(tNew
                .getExpirationTime())));
        }

        return mods;
    }
}
