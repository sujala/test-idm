package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.DomainDao;
import com.rackspace.idm.domain.entity.Domain;
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

    @InjectMocks
    DefaultDomainService defaultDomainService = new DefaultDomainService();
    @Mock
    DomainDao domainDao;
    @Mock
    private TenantService tenantService;

    DefaultDomainService spy;

    @Before
    public void setUp() throws Exception {
        tenantService = mock(TenantService.class);
        spy = spy(defaultDomainService);
        spy.setTenantService(tenantService);
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
    public void removeTenantFromDomain_validDomain() throws Exception{
        Domain domain = new Domain();
        domain.setDomainId("1");
        domain.setName("domain");
        List<String> tenantIds = new ArrayList<String>();
        when(spy.getDomain("1")).thenReturn(domain);
        when(spy.setTenantIdList(domain, "tenant1")).thenReturn(tenantIds);
        defaultDomainService.removeTenantFromDomain("tenant1", "1");
        verify(domainDao).updateDomain(domain);
    }

    @Test(expected = NotFoundException.class)
    public void checkAndGetDomain_invalidDomain_throwsNotFoundException() throws Exception{
        when(spy.getDomain("1")).thenReturn(null);
        defaultDomainService.checkAndGetDomain("1");
    }

    @Test
    public void createNewDoamin_validDomainId_returnsDomainId() throws Exception{
        String domainId = defaultDomainService.createNewDomain("1");
        assertThat("verify DomainId",domainId,equalTo("1"));
    }

    @Test
    public void deleteDomain_verifyDelete() throws Exception{
        spy.deleteDomain("1");
        verify(domainDao).deleteDomain("1");
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
