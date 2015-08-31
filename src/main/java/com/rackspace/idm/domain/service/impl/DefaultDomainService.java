package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.DomainDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.ForbiddenException;
import com.rackspace.idm.exception.NotFoundException;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: matt.colton
 * Date: 8/6/12
 * Time: 3:42 PM
 * To change this template use File | Settings | File Templates.
 */
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
        if(domain == null)
            throw new NotFoundException("Domain could not be found");
        if(!domain.getEnabled())
            throw new ForbiddenException("Cannot add tenant to disabled domain");

        final Tenant tenant = tenantService.checkAndGetTenant(tenantId);

        String[] curDomainTenantIds = domain.getTenantIds();
        if (!ArrayUtils.contains(curDomainTenantIds, tenantId)) {
            final List<String> tenantIds = setTenantIdList(domain, tenantId);
            tenantIds.add(tenantId);
            domain.setTenantIds(tenantIds.toArray(new String[tenantIds.size()]));
            domainDao.updateDomain(domain);
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

            //if tenant previously pointed to old domain, must remove association. Must call this AFTER updating the tenant to new tenantId
            if (StringUtils.isNotBlank(oldDomainId)) {
                removeTenantFromDomain(tenantId, oldDomainId);
            }
        }
    }

    @Override
    public void removeTenantFromDomain(String tenantId, String domainId) {
        Domain domain = getDomain(domainId);
        if(domain == null)
            throw new NotFoundException("Domain could not be found");

        List<String> tenantIds = setTenantIdList(domain, tenantId);
        domain.setTenantIds(tenantIds.toArray(new String[tenantIds.size()]));
        domainDao.updateDomain(domain);
    }

    @Override
    public Domain checkAndGetDomain(String domainId) {
        Domain domain = this.getDomain(domainId);
        if (domain == null) {
            String errMsg = String.format("Domain with id: '%s' was not found.", domainId);
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
