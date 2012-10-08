package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.DomainDao;
import com.rackspace.idm.domain.dao.TenantDao;
import com.rackspace.idm.domain.entity.Domain;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.ForbiddenException;
import com.rackspace.idm.exception.NotFoundException;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Created by IntelliJ IDEA.
 * User: matt.colton
 * Date: 8/13/12
 * Time: 10:30 AM
 * To change this template use File | Settings | File Templates.
 */
public class DefaultDomainServiceTest {

    DefaultDomainService defaultDomainService;
    private DomainDao domainDao = mock(DomainDao.class);
    private TenantService tenantService = mock(TenantService.class);
    DefaultDomainService spy;

    @Before
    public void setUp() throws Exception {
        defaultDomainService = new DefaultDomainService(domainDao);
        defaultDomainService.setTenantService(tenantService);
        spy = spy(defaultDomainService);
    }

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

    @Test(expected = NotFoundException.class)
    public void addTenantsToDomain_nullDomainId_throwsNotFoundException() throws Exception{
        defaultDomainService.addTenantToDomain(null,null);
    }

    @Test(expected = ForbiddenException.class)
    public void addTenantsToDomain_disabledDomain_throwsForbiddenException() throws Exception{
        Domain domain = new Domain();
        domain.setDomainId("1");
        domain.setEnabled(false);
        when(domainDao.getDomain(anyString())).thenReturn(domain);
        defaultDomainService.addTenantToDomain(null, null);
    }

    @Test
    public void addTenantsToDomain_validTenant_returns() throws Exception{
        Domain domain = new Domain();
        domain.setDomainId("1");
        domain.setEnabled(true);
        when(domainDao.getDomain(anyString())).thenReturn(domain);
        when(tenantService.getTenant(anyString())).thenReturn(null);
        defaultDomainService.addTenantToDomain(null, null);
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




}
