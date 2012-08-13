package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.DomainDao;
import com.rackspace.idm.domain.entity.Domain;
import com.rackspace.idm.exception.BadRequestException;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;

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
    DefaultDomainService spy;

    @Before
    public void setUp() throws Exception {
        defaultDomainService = new DefaultDomainService(domainDao);
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

}
