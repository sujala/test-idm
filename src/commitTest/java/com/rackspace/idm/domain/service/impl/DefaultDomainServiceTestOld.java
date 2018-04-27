package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.DomainDao;
import com.rackspace.idm.domain.entity.Domain;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.ForbiddenException;
import com.rackspace.idm.exception.NotFoundException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: matt.colton
 * Date: 8/13/12
 * Time: 10:30 AM
 * To change this template use File | Settings | File Templates.
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultDomainServiceTestOld {

    @Mock
    DomainDao domainDao;

    @Mock
    TenantService tenantService;

    @InjectMocks
    DefaultDomainService defaultDomainService = new DefaultDomainService();

    @Test(expected = BadRequestException.class)
    public void addDomain_nullDomain_throwIllegalArgumentException() throws Exception {
        when(domainDao.getDomain(null)).thenReturn(new Domain());
        defaultDomainService.addDomain(null);
    }

    @Test
    public void getDomain() throws Exception {
        defaultDomainService.getDomain(null);
        verify(domainDao).getDomain(null);
    }

    @Test(expected = BadRequestException.class)
    public void addDomain_invalidDomain_returnBadRequestException() throws Exception{
        Domain domain = new Domain();
        defaultDomainService.addDomain(domain);
    }

    @Test(expected = BadRequestException.class)
    public void addDomain_blankName_throwsBadRequestException() throws Exception{
        Domain domain = new Domain();
        domain.setDomainId("1");
        defaultDomainService.addDomain(domain);
    }

    @Test
    public void addDomain_validDomain_returns() throws Exception{
        Domain domain = new Domain();
        domain.setDomainId("1");
        domain.setName("domain");
        defaultDomainService.addDomain(domain);
    }

    @Test(expected = BadRequestException.class)
    public void updateDomain_invalidDomain_returnBadRequestException() throws Exception{
        Domain domain = new Domain();
        defaultDomainService.updateDomain(domain);
    }

    @Test(expected = BadRequestException.class)
    public void updateDomain_nullDomain_returnBadRequestException() throws Exception{
        defaultDomainService.updateDomain(null);
    }

    @Test(expected = BadRequestException.class)
    public void updateDomain_blankName_throwsBadRequestException() throws Exception{
        Domain domain = new Domain();
        domain.setDomainId("1");
        defaultDomainService.updateDomain(domain);
    }

    @Test
    public void updateDomain_validDomain_returns() throws Exception{
        Domain domain = new Domain();
        domain.setDomainId("1");
        domain.setName("domain");
        defaultDomainService.updateDomain(domain);
    }

    @Test
    public void addTenantsToDomain_validTenant_returns() throws Exception {
        Domain domain = new Domain();
        domain.setDomainId("1");
        domain.setEnabled(true);
        Tenant tenant = new Tenant();
        when(domainDao.getDomain(anyString())).thenReturn(domain);
        when(tenantService.checkAndGetTenant(anyString())).thenReturn(tenant);
        defaultDomainService.addTenantToDomain(null, "1");
    }

    @Test(expected = NotFoundException.class)
    public void removeTenantFromDomain_invalidDomain_throwsNotFoundException() throws Exception{
        defaultDomainService.removeTenantFromDomain(null, null);
    }

    @Test
    public void createNewDoamin_validDomainId_returnsDomainId() throws Exception{
        String domainId = defaultDomainService.createNewDomain("1");
        assertThat("verify DomainId",domainId,equalTo("1"));
    }

    @Test(expected = BadRequestException.class)
    public void addDomain_invalidIdCharacters_throwsBadRequest() throws Exception {
        Domain domain = new Domain();
        domain.setDomainId("!nvalid");
        defaultDomainService.addDomain(domain);
    }

    @Test(expected = BadRequestException.class)
    public void addDomain_invalidNameCharacters_throwsBadRequest() throws Exception {
        Domain domain = new Domain();
        domain.setDomainId("1");
        domain.setName("!nvalid");
        defaultDomainService.addDomain(domain);
    }

}
