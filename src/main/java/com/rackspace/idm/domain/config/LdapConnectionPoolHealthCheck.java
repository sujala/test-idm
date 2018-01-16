package com.rackspace.idm.domain.config;

import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.rackspace.idm.domain.dao.impl.LdapRepository.ATTR_OBJECT_CLASS;
import static com.rackspace.idm.domain.dao.impl.LdapRepository.OBJECTCLASS_INET_ORG_PERSON;

@Component
public class LdapConnectionPoolHealthCheck extends LDAPConnectionPoolHealthCheck {

    private static final Logger logger = LoggerFactory.getLogger(LdapConnectionPoolHealthCheck.class);

    @Autowired
    private IdentityConfig identityConfig;

    private static final String LDAP_SEARCH_ERROR_MESSAGE = "Unable to locate base object: %s";

    /**
     * Performs any desired processing to determine whether the provided new
     * connection is available to be checked out and used for processing
     * operations.
     *
     * This check will be triggered when new LDAP connections are created due to
     * old connections being close when reaching max age.
     **/
    @Override
    public void ensureNewConnectionValid(LDAPConnection connection) throws LDAPException {
        if(identityConfig.getReloadableConfig().getEnableLdapHealthCheckNewConnection()) {
            ensureValidConnection(connection);
        }
    }

   /**
    * Performs any desired processing to determine whether the provided
    * connection is valid and should continue to be made available for
    * processing operations.
    *
    * This check will be triggered by the LDAP health check which is schedule to run on intervals
    * set by ldap.server.pool.health.check.interval static property (Default: 60000ms).
    * When the health check runs, all available connections will be checked.
    **/
    @Override
    public void ensureConnectionValidForContinuedUse(LDAPConnection connection) throws LDAPException {
        if(identityConfig.getReloadableConfig().getEnableLdapHealthCheckConnectionForContinuedUse()) {
            ensureValidConnection(connection);
        }
    }

    private void ensureValidConnection(LDAPConnection connection) throws LDAPException {
        String bindDN = identityConfig.getStaticConfig().getLDAPServerBindDN();
        SearchResult searchResult = connection.search(bindDN, SearchScope.BASE, searchFilterGeLdapINetOrgPerson());
        if (searchResult == null || searchResult.getEntryCount() < 1) {
            logger.error(Thread.currentThread().getStackTrace()[2].getMethodName() + ": " + String.format(LDAP_SEARCH_ERROR_MESSAGE, bindDN));
            throw new LDAPException(ResultCode.OTHER, String.format(LDAP_SEARCH_ERROR_MESSAGE, bindDN));
        }
    }

     public Filter searchFilterGeLdapINetOrgPerson() {
        return new LdapRepository.LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_INET_ORG_PERSON)
                .build();
    }
}
