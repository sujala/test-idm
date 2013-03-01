package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.api.resource.pagination.PaginatorContext;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.AuthorizationService;
import org.springframework.stereotype.Component;

import com.rackspace.idm.domain.dao.DomainDao;
import com.rackspace.idm.domain.service.DomainService;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.*;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

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
public class DefaultDomainService implements DomainService{

    @Autowired
    private Configuration config;

    @Autowired
    private UserService userService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private AuthorizationService authorizationService;

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
        Domain domain = getDomain(domainId);
        if(domain == null)
            throw new NotFoundException("Domain could not be found");
        if(!domain.isEnabled())
            throw new ForbiddenException("Cannot add tenant to disabled domain");

        tenantService.checkAndGetTenant(tenantId);

        List<String> tenantIds = setTenantIdList(domain, tenantId);
        tenantIds.add(tenantId);
        domain.setTenantIds(tenantIds.toArray(new String[tenantIds.size()]));
        domainDao.updateDomain(domain);
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
    public List<Domain> getDomainsForTenants(List<Tenant> tenantIds) {
        return domainDao.getDomainsForTenant(tenantIds);
    }

    @Override
    public Users getUsersByDomainId(String domainId) {
        FilterParam[] filters = new FilterParam[]{new FilterParam(FilterParam.FilterParamName.DOMAIN_ID, domainId)};
        return userService.getAllUsers(filters);
    }

    @Override
    public Users getUsersByDomainId(String domainId, boolean enabled) {

        FilterParam[] filters = new FilterParam[]
            {
                new FilterParam(FilterParam.FilterParamName.DOMAIN_ID, domainId),
                new FilterParam(FilterParam.FilterParamName.ENABLED, String.valueOf(enabled))
            };
        return userService.getAllUsers(filters);
    }

    @Override
    public List<User> getDomainAdmins(String domainId) {
        return filterUserAdmins(userService.getUsersInDomain(domainId));
    }

    @Override
    public List<User> getDomainAdmins(String domainId, boolean enabled) {
        return filterUserAdmins(userService.getUsersInDomain(domainId, enabled));
    }

    private List<User> filterUserAdmins(List<User> userList) {
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
