package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.LDAPComponent;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.IdentityProviderDao;
import com.rackspace.idm.domain.entity.ApprovedDomainGroupEnum;
import com.rackspace.idm.domain.entity.IdentityProvider;
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

    public String[] getSearchAttributes(){
        return ATTR_IDENTITY_PROVIDER_ATTRIBUTES;
    }

    @Override
    public String getBaseDn() {
        return EXTERNAL_PROVIDERS_BASE_DN;
    }

    @Override
    public IdentityProvider getIdentityProviderByUri(String uri) {
        return getObject(searchByUriFilter(uri));
    }

    @Override
    public IdentityProvider getIdentityProviderById(String Id) {
        return getObject(searchByIdFilter(Id));
    }

    @Override
    public IdentityProvider getIdentityProviderByName(String name) {
        return getObject(searchByNameFilter(name));
    }

    @Override
    public IdentityProvider getIdentityProviderByEmailDomain(String emailDomain) {
        return getObject(searchFilterGetIdentityProviderByEmailDomain(emailDomain));
    }

    @Override
    public IdentityProvider getIdentityProviderWithMetadataById(String id) {
        return getObject(searchByIdFilter(id), getBaseDn(), SearchScope.ONE, LdapRepository.ATTR_DEFAULT_SEARCH_ATTRIBUTES);
    }

    @Override
    public IdentityProvider getIdentityProviderApprovedForDomain(String name, String domainId) {
        return getObject(searchFilterGetIdentityProviderApprovedForDomain(name, domainId));
    }

    @Override
    public IdentityProvider getIdentityProviderExplicitlyApprovedForDomain(String name, String domainId) {
        return getObject(searchFilterGetIdentityProviderExplicitlyApprovedForDomain(name, domainId));
    }

    @Override
    public IdentityProvider getIdentityProvidersExplicitlyApprovedForAnyDomain(String name) {
        return getObject(searchFilterGetIdentityProviderExplicitlyApprovedForAnyDomain(name));
    }

    @Override
    public IdentityProvider getIdentityProviderApprovedForDomainByIssuer(String issuer, String domainId) {
        return getObject(searchFilterGetIdentityProviderApprovedForDomainByIssuer(issuer, domainId));
    }

    @Override
    public IdentityProvider getIdentityProviderExplicitlyApprovedForDomainByIssuer(String issuer, String domainId) {
        return getObject(searchFilterGetIdentityProviderExplicitlyApprovedForDomainByIssuer(issuer, domainId));
    }

    @Override
    public IdentityProvider getIdentityProvidersExplicitlyApprovedForAnyDomainByIssuer(String issuer) {
        return getObject(searchFilterGetIdentityProviderExplicitlyApprovedForAnyDomainByIssuer(issuer));
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
    public List<IdentityProvider> findIdentityProvidersExplicitlyApprovedForDomain(String domainId) {
        int maxAllowed = identityConfig.getReloadableConfig().getMaxListIdentityProviderSize();
        List<IdentityProvider> providers;
        try {
            providers = getUnpagedUnsortedObjects(searchFilterGetIdentityProvidersExplicitlyApprovedForDomain(domainId), getBaseDn(), SearchScope.SUB, maxAllowed);
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
    public List<IdentityProvider> findIdentityProvidersExplicitlyApprovedForAnyDomain() {
        int maxAllowed = identityConfig.getReloadableConfig().getMaxListIdentityProviderSize();
        List<IdentityProvider> providers;
        try {
            providers = getUnpagedUnsortedObjects(searchFilterGetIdentityProvidersExplicitlyApprovedForAnyDomain(), getBaseDn(), SearchScope.SUB, maxAllowed);
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

    Filter searchByIdFilter(String id) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OU, id)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_EXTERNALPROVIDER).build();
    }

    Filter searchByNameFilter(String name) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_NAME, name)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_EXTERNALPROVIDER).build();
    }

    Filter searchForAll() {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_EXTERNALPROVIDER).build();
    }

    /**
     * Either the domain is in the list of approvedDomains OR the domainGroup is global
     * @param name
     * @param domainId
     * @return
     */
    private Filter searchFilterGetIdentityProviderApprovedForDomain(String name, String domainId) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_NAME, name)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_EXTERNALPROVIDER)
                .addOrAttributes(Arrays.asList(Filter.createEqualityFilter(ATTR_APPROVED_DOMAIN_IDS, domainId)
                        , Filter.createEqualityFilter(ATTR_APPROVED_DOMAIN_GROUP, ApprovedDomainGroupEnum.GLOBAL.getStoredVal())))
                .build();
    }

    /**
     * Search filter for IDP where rsApprovedDomainIds contains domainId
     *
     * @param name
     * @param domainId
     * @return
     */
    private Filter searchFilterGetIdentityProviderExplicitlyApprovedForDomain(String name, String domainId) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_NAME, name)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_EXTERNALPROVIDER)
                .addEqualAttribute(ATTR_APPROVED_DOMAIN_IDS, domainId)
                .build();
    }

    /**
     * Search filter for IDPs where rsApprovedDomainIds is present
     *
     * @return
     */
    private Filter searchFilterGetIdentityProviderExplicitlyApprovedForAnyDomain(String name) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_NAME, name)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_EXTERNALPROVIDER)
                .addPresenceAttribute(ATTR_APPROVED_DOMAIN_IDS)
                .build();
    }

    /**
     * Search by issuer and domainId where either the domain is in the list of approvedDomains OR the domainGroup is global
     * @param issuer
     * @param domainId
     * @return
     */
    private Filter searchFilterGetIdentityProviderApprovedForDomainByIssuer(String issuer, String domainId) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_LABELED_URI, issuer)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_EXTERNALPROVIDER)
                .addOrAttributes(Arrays.asList(Filter.createEqualityFilter(ATTR_APPROVED_DOMAIN_IDS, domainId)
                        , Filter.createEqualityFilter(ATTR_APPROVED_DOMAIN_GROUP, ApprovedDomainGroupEnum.GLOBAL.getStoredVal())))
                .build();
    }

    /**
     * Search filter for IDP where rsApprovedDomainIds contains domainId by issuer
     *
     * @param issuer
     * @param domainId
     * @return
     */
    private Filter searchFilterGetIdentityProviderExplicitlyApprovedForDomainByIssuer(String issuer, String domainId) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_LABELED_URI, issuer)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_EXTERNALPROVIDER)
                .addEqualAttribute(ATTR_APPROVED_DOMAIN_IDS, domainId)
                .build();
    }

    /**
     * Search filter for IDPs where rsApprovedDomainIds is present
     *
     * @return
     */
    private Filter searchFilterGetIdentityProviderExplicitlyApprovedForAnyDomainByIssuer(String issuer) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_LABELED_URI, issuer)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_EXTERNALPROVIDER)
                .addPresenceAttribute(ATTR_APPROVED_DOMAIN_IDS)
                .build();
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

    /**
     * Search filter for IDPs where rsApprovedDomainIds contains domainId
     *
     * @param domainId
     * @return
     */
    Filter searchFilterGetIdentityProvidersExplicitlyApprovedForDomain(String domainId) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_EXTERNALPROVIDER)
                .addEqualAttribute(ATTR_APPROVED_DOMAIN_IDS, domainId)
                .build();
    }

    /**
     * Search filter for IDPs where rsApprovedDomainIds is present
     *
     * @return
     */
    Filter searchFilterGetIdentityProvidersExplicitlyApprovedForAnyDomain() {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_EXTERNALPROVIDER)
                .addPresenceAttribute(ATTR_APPROVED_DOMAIN_IDS)
                .build();
    }

    /**
     * Search filter for IDP where rsEmailDomains contains emailDomain
     *
     * @param emailDomain
     * @return
     */
    private Filter searchFilterGetIdentityProviderByEmailDomain(String emailDomain) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_EXTERNALPROVIDER)
                .addEqualAttribute(ATTR_EMAIL_DOMAINS, emailDomain)
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
    public void updateIdentityProviderAsIs(IdentityProvider identityProvider) {
        updateObjectAsIs(identityProvider);
    }

    @Override
    public void deleteIdentityProviderById(String id) {
        deleteObject(searchByIdFilter(id));
    }

    @Override
    public void addObject(IdentityProvider object) {
        super.addObject(object);
        addOrganizationalUnit(object.getUniqueId(), LdapRepository.EXTERNAL_PROVIDERS_USER_CONTAINER_NAME);
    }
}
