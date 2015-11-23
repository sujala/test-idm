package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.ErrorCodes;
import com.rackspace.idm.annotation.LDAPComponent;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.IdentityProviderDao;
import com.rackspace.idm.domain.entity.ApprovedDomainGroupEnum;
import com.rackspace.idm.domain.entity.IdentityProvider;
import com.rackspace.idm.exception.IdmException;
import com.rackspace.idm.exception.SizeLimitExceededException;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPSearchException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.List;

@LDAPComponent
public class LdapIdentityProviderRepository extends LdapGenericRepository<IdentityProvider> implements IdentityProviderDao {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private IdentityConfig identityConfig;

    @Override
    public String getBaseDn() {
        return EXTERNAL_PROVIDERS_BASE_DN;
    }

    @Override
    public IdentityProvider getIdentityProviderByUri(String uri) {
        return getObject(searchByUriFilter(uri));
    }

    @Override
    public IdentityProvider getIdentityProviderByName(String name) {
        return getObject(searchByNameFilter(name));
    }

    @Override
    public List<IdentityProvider> findIdentityProvidersApprovedForDomain(String domainId) {
        int maxAllowed = identityConfig.getReloadableConfig().getMaxListIdentityProviderSize();
        List<IdentityProvider> providers;
        try {
            providers = getUnpagedUnsortedObjects(searchFilterGetIdentityProvidersApprovedForDomain(domainId), getBaseDn(), SearchScope.SUB, maxAllowed);
            return providers;
        } catch (LDAPSearchException ldapEx) {
            if (ldapEx.getResultCode() == ResultCode.SIZE_LIMIT_EXCEEDED) {
                logger.debug(String.format("Aborting loading identity providers. Result size exceeded limit of %d", maxAllowed), ldapEx);
                throw new SizeLimitExceededException(String.format("Result size exceeded. Results limited to " + maxAllowed + " providers."), ldapEx);
            } else {
                throw new IllegalStateException(ldapEx);
            }
        }
    }

    @Override
    public List<IdentityProvider> findAllIdentityProviders() {
        int maxAllowed = identityConfig.getReloadableConfig().getMaxListIdentityProviderSize();
        List<IdentityProvider> providers = null;
        try {
            providers = getUnpagedUnsortedObjects(searchForAll(), getBaseDn(), SearchScope.SUB, maxAllowed);
            return providers;
        } catch (LDAPSearchException ldapEx) {
            if (ldapEx.getResultCode() == ResultCode.SIZE_LIMIT_EXCEEDED) {
                logger.debug(String.format("Aborting loading identity providers. Result size exceeded limit of %d", maxAllowed), ldapEx);
                throw new SizeLimitExceededException(String.format("Result size exceeded. Results limited to %d providers.", maxAllowed), ldapEx);
            } else {
                throw new IllegalStateException(ldapEx);
            }
        }
    }

    Filter searchByUriFilter(String uri) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_URI, uri)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_EXTERNALPROVIDER).build();
    }

    Filter searchByNameFilter(String name) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OU, name)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_EXTERNALPROVIDER).build();
    }

    Filter searchForAll() {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_EXTERNALPROVIDER).build();
    }

    /**
     * Either the domain is in the list of approvedDomains OR the domainGroup is global
     * @param domainId
     * @return
     */
    Filter searchFilterGetIdentityProvidersApprovedForDomain(String domainId) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_EXTERNALPROVIDER)
                .addOrAttributes(Arrays.asList(Filter.createEqualityFilter(ATTR_APPROVED_DOMAIN_IDS, domainId)
                        , Filter.createEqualityFilter(ATTR_APPROVED_DOMAIN_GROUP, ApprovedDomainGroupEnum.GLOBAL.getStoredVal())))
                .build();
    }


    @Override
    public void addIdentityProvider(IdentityProvider identityProvider) {
        addObject(identityProvider);
    }

    @Override
    public void updateIdentityProvider(IdentityProvider identityProvider) {
        updateObject(identityProvider);
    }

    @Override
    public void deleteIdentityProviderById(String id) {
        deleteObject(searchByNameFilter(id));
    }

    @Override
    public void addObject(IdentityProvider object) {
        super.addObject(object);
        addOrganizationalUnit(object.getUniqueId(), LdapRepository.EXTERNAL_PROVIDERS_USER_CONTAINER_NAME);
    }
}
