package com.rackspace.idm.domain.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.DomainDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.*;
import java.util.regex.Pattern;

@Component
public class DefaultDomainService implements DomainService {

    @Autowired
    private Configuration config;

    @Autowired
    private IdentityConfig identityConfig;

    @Autowired
    private UserService userService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private ScopeAccessService scopeAccessService;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private IdentityUserService identityUserService;

    public static final String DOMAIN_CANNOT_BE_NULL = "Domain cannot be null";
    public static final String DOMAIN_ID_CANNOT_BE_NULL = "Domain ID cannot be null";
    public static final String DOMAIN_NAME_CANNOT_BE_NULL = "Domain name cannot be null or empty";
    public static final String DOMAIN_NOT_FOUND_ERROR_MESSGAE = "Domain with ID %s not found.";

    @Autowired
    private DomainDao domainDao;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void addDomain(Domain domain) {
        if(domain == null){
            throw new BadRequestException(DOMAIN_CANNOT_BE_NULL);
        }
        if(StringUtils.isBlank(domain.getDomainId())) {
            throw new BadRequestException(DOMAIN_ID_CANNOT_BE_NULL);
        }
        if(StringUtils.isBlank(domain.getName())) {
            throw new BadRequestException(DOMAIN_NAME_CANNOT_BE_NULL);
        }
        if(StringUtils.isBlank(domain.getDescription())) {
            domain.setDescription(null);
        }
        if(StringUtils.isBlank(domain.getRackspaceCustomerNumber())) {
            domain.setRackspaceCustomerNumber(null);
        }
        if(domainDao.domainExistsWithName(domain.getName())) {
            throw new DuplicateException(String.format("Domain with name %s already exists.", domain.getName()));
        }
        // Default to true if enabled attribute is not specified by the caller
        if (domain.getEnabled() == null) {
           domain.setEnabled(true);
        }
        verifyDomain(domain);
        logger.info("Adding Domain: {}", domain);
        domainDao.addDomain(domain);
    }

    @Override
    public Domain getDomain(String domainId) {
        return domainDao.getDomain(domainId);
    }

    @Override
    public PaginatorContext<Domain> getDomains(int offset, int limit) {
        return domainDao.getAllDomainsPaged(offset, limit);
    }

    @Override
    public PaginatorContext<Domain> getDomainsByRCN(String rcn, Integer marker, Integer limit) {
        return domainDao.getRCNDomainsPaged(rcn, marker, limit);
    }

    @Override
    public void updateDomain(Domain domain) {
        if(domain == null){
            throw new BadRequestException(DOMAIN_CANNOT_BE_NULL);
        }
        if(StringUtils.isBlank(domain.getDomainId())) {
            throw new BadRequestException(DOMAIN_ID_CANNOT_BE_NULL);
        }
        if(StringUtils.isBlank(domain.getName())) {
            throw new BadRequestException(DOMAIN_NAME_CANNOT_BE_NULL);
        }
        if(domainDao.domainExistsWithNameAndNotId(domain.getName(), domain.getDomainId())) {
            throw new DuplicateException(String.format("Domain with name %s already exists.", domain.getName()));
        }
        verifyDomain(domain);
        domainDao.updateDomain(domain);
    }

    @Override
    public void deleteDomain(String domainId) {
        domainDao.deleteDomain(domainId);
    }

    @Override
    public void addTenantToDomain(String tenantId, String domainId) {
        final Domain domain = getDomain(domainId);
        if(domain == null) {
            String errMsg = String.format(DOMAIN_NOT_FOUND_ERROR_MESSGAE, domainId);
            throw new NotFoundException(errMsg);
        }

        final Tenant tenant = tenantService.checkAndGetTenant(tenantId);

        addTenantToDomain(tenant, domain);
    }

    private void addTenantToDomain(Tenant tenant, Domain newDomain) {
        Assert.notNull(tenant);
        Assert.notNull(newDomain);

        String tenantId = tenant.getTenantId();
        String domainId = newDomain.getDomainId();

        String[] curDomainTenantIds = newDomain.getTenantIds();
        if (!ArrayUtils.contains(curDomainTenantIds, tenantId)) {
            final List<String> tenantIds = setTenantIdList(newDomain, tenantId);
            tenantIds.add(tenantId);
            newDomain.setTenantIds(tenantIds.toArray(new String[tenantIds.size()]));
            domainDao.updateDomain(newDomain);
        }

        //update the tenant if it doesn't already point to this domain
        String oldDomainId = tenant.getDomainId();
        if (!domainId.equals(oldDomainId)) {
            try {
                tenant.setDomainId(domainId);
                tenantService.updateTenant(tenant);
            } catch (Exception e) {
                logger.error(String.format("Inconsistent state. Domain '%s' was updated to point to tenant '%s', but tenant does not point to domain", domainId, tenantId), e);
                throw new RuntimeException(e);
            }

            //if tenant previously pointed to old domain, must remove association from that domain. Must call this
            // AFTER updating the tenant to new tenantId
            if (StringUtils.isNotBlank(oldDomainId)) {
                Domain oldDomain = domainDao.getDomain(oldDomainId);
                if (oldDomain != null) {
                    removeTenantFromDomain(tenant, oldDomain);
                }
            }
        }
    }

    @Override
    public void removeTenantFromDomain(String tenantId, String domainId) {
        Domain domain = getDomain(domainId);
        if(domain == null) {
            String errMsg = String.format(DOMAIN_NOT_FOUND_ERROR_MESSGAE, domainId);
            throw new NotFoundException(errMsg);
        }
        final Tenant tenant = tenantService.getTenant(tenantId);
        if (tenant == null) {
            /*
            do this to allow for case where domain contains pointer to invalid tenant (since LDAP doesn't maintain
            ref integrity. Don't need to account for case where tenant points to invalid domain because can just add
            tenant to new domain
             */
            String[] curDomainTenantIds = domain.getTenantIds();
            if (ArrayUtils.contains(curDomainTenantIds, tenantId)) {
                List<String> tenantIds = setTenantIdList(domain, tenantId);
                domain.setTenantIds(tenantIds.toArray(new String[tenantIds.size()]));
                domainDao.updateDomain(domain);
            }
        } else {
            try {
                removeTenantFromDomain(tenant, domain);
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException(ex.getMessage(), ex);
            }
        }
    }

    private void removeTenantFromDomain(Tenant tenant, Domain domain) {
        Assert.notNull(tenant);
        Assert.notNull(domain);

        String tenantId = tenant.getTenantId();
        String domainId = domain.getDomainId();
        String defaultDomainId = identityConfig.getReloadableConfig().getTenantDefaultDomainId();

        //can only remove a tenant from the default domain if the tenant is associated with a different domain
        if (defaultDomainId.equals(domain.getDomainId()) && domainId.equals(tenant.getDomainId())) {
            throw new IllegalArgumentException("Can not remove a tenant from the default domain. To change the domain add the tenant to a new domain");
        }

        //remove tenant from specified domain
        String[] curDomainTenantIds = domain.getTenantIds();
        if (ArrayUtils.contains(curDomainTenantIds, tenantId)) {
            List<String> tenantIds = setTenantIdList(domain, tenantId);
            domain.setTenantIds(tenantIds.toArray(new String[tenantIds.size()]));
            domainDao.updateDomain(domain);
        }

        //if the tenant currently points to the domain that it's to be removed from, must reset the tenant to the default domain
        //since a tenant must always belong to a domain
        if (domainId.equals(tenant.getDomainId())) {
            Domain defaultDomain = domainDao.getDomain(defaultDomainId);
            addTenantToDomain(tenant, defaultDomain);
        }
    }

    @Override
    public Domain checkAndGetDomain(String domainId) {
        Domain domain = this.getDomain(domainId);
        if (domain == null) {
            String errMsg = String.format(DOMAIN_NOT_FOUND_ERROR_MESSGAE, domainId);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }
        return domain;
    }

    @Override
    public String createNewDomain(String domainId){
        try {
            Domain domain = new Domain();
            domain.setDomainId(domainId);
            domain.setEnabled(true);
            domain.setName(domainId);
            domain.setDescription("Default Domain");
            addDomain(domain);
            return domain.getDomainId();
        }
        catch(DuplicateException ex){
            logger.error("Domain already exists.");
            return domainId;
        }
        catch(Exception ex){
            throw new BadRequestException("Domain could not be created.");
        }
    }

    @Override
    public Iterable<Domain> getDomainsForTenants(List<Tenant> tenantIds) {
        return domainDao.getDomainsForTenant(tenantIds);
    }

    @Override
    public void expireAllTokenInDomain(String domainId) {
        for(EndUser user : identityUserService.getEndUsersByDomainId(domainId)) {
            try {
                scopeAccessService.expireAllTokensForUserById(user.getId());
            } catch (Exception ex) {
                String errMsg = String.format("ACTION NEEDED: Failed to expired all tokens for user %s. This user's tokens must be manually expired because the domain was disabled", user.getUsername());
                logger.error(errMsg, ex);
            }
        }
    }

    @Override
    public void deleteDomainPasswordPolicy(String domainId) {
        Domain domain = checkAndGetDomain(domainId);
        try {
            domain.setPasswordPolicy(null);
            domainDao.updateDomainAsIs(domain);
        } catch (JsonProcessingException e) {
            // Should never get thrown, but catch all the same
            logger.error(String.format("Error nulling out password policy for domain '%s'", domainId), e);
            throw new IllegalStateException("Error nulling out password policy");
        }
    }

    @Override
    public Iterable<User> getUsersByDomainId(String domainId) {
        return userService.getUsersWithDomain(domainId);
    }

    @Override
    public Iterable<User> getUsersByDomainIdAndEnabledFlag(String domainId, boolean enabled) {
        return userService.getUsersWithDomainAndEnabledFlag(domainId, enabled);
    }

    @Override
    public List<User> getDomainAdmins(String domainId) {
        return filterUserAdmins(userService.getUsersWithDomain(domainId));
    }

    @Override
    public List<User> getDomainSuperAdmins(String domainId) {
        return filterSuperAdmins(userService.getUsersWithDomain(domainId));
    }

    @Override
    public List<User> getEnabledDomainAdmins(String domainId) {
        return filterUserAdmins(userService.getUsersWithDomainAndEnabledFlag(domainId, true));
    }

    @Override
    public String getDomainUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public void updateDomainUserAdminDN(User user) {
        Validate.notNull(user);
        Validate.notNull(user.getUniqueId());
        Validate.notNull(user.getDomainId());
        Validate.notEmpty(user.getRoles());

        // Verify user has user-admin role
        TenantRole userAdminRole = null;
        for (TenantRole tenantRole : user.getRoles()) {
            if (IdentityUserTypeEnum.USER_ADMIN.getRoleName().equalsIgnoreCase(tenantRole.getName())) {
                userAdminRole = tenantRole;
                break;
            }
        }

        if (userAdminRole == null) {
            throw new IllegalArgumentException("Supplied user is not a user-admin, cannot update domain's userAdminDN.");
        }

        Domain updateDomain = getDomain(user.getDomainId());
        updateDomain.setUserAdminDN(user.getDn());
        updateDomain(updateDomain);
    }

    @Override
    public void removeDomainUserAdminDN(User user) {
        Validate.notNull(user);
        Validate.notNull(user.getDomainId());
        Validate.notNull(user.getUniqueId());

        Domain domain = getDomain(user.getDomainId());

        // Verify that userAdminDn being delete matches the user's DN.
        if (domain != null && domain.getUserAdminDN() != null && domain.getUserAdminDN().equals(user.getDn())) {
            domain.setUserAdminDN(null);
            updateDomain(domain);
        }
    }

    @Override
    public Iterable<Domain> findDomainsWithRcn(String rcn) {
        if (StringUtils.isEmpty(rcn)) {
            return Collections.emptyList();
        }
        return domainDao.findDomainsWithRcn(rcn);
    }

    private List<User> filterSuperAdmins(Iterable<User> userList) {
        List<User> superAdmins = new ArrayList<User>();
        for (User user : userList) {
            if (authorizationService.hasServiceAdminRole(user) || authorizationService.hasIdentityAdminRole(user)) {
                superAdmins.add(user);
            }
        }
        return superAdmins;
    }

    private List<User> filterUserAdmins(Iterable<User> userList) {
        List<User> userAdmins = new ArrayList<User>();
        for (User user : userList) {
            if (authorizationService.hasUserAdminRole(user)) {
                userAdmins.add(user);
            }
        }
        return userAdmins;
    }

    List<String> setTenantIdList(Domain domain, String tenantId) {
        List<String> tenantIds = new ArrayList<String>();
        if(domain.getTenantIds() != null){
            for(String tenant : domain.getTenantIds()){
                if(!tenant.equals(tenantId)){
                    tenantIds.add(tenant);
                }
            }
        }
        return tenantIds;
    }

    private void verifyDomain(Domain domain) {
        Pattern alphaNumericColonHyphen = Pattern.compile("[a-zA-Z0-9:-]*");
        Pattern alphaNumericColonHyphenSpace = Pattern.compile("[a-zA-Z0-9\\s:-]*");
        if (!alphaNumericColonHyphen.matcher(domain.getDomainId()).matches()) {
            throw new BadRequestException("Domain ID has invalid characters.");
        }
        if (!alphaNumericColonHyphenSpace.matcher(domain.getName()).matches()) {
            throw new BadRequestException("Domain name has invalid characters.");
        }
    }

    public void setTenantService(TenantService tenantService) {
        this.tenantService = tenantService;
    }
}
