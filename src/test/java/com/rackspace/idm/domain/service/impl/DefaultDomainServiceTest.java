package com.rackspace.idm.domain.service.impl;

import org.junit.runner.RunWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.runners.MockitoJUnitRunner;

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
@RunWith(MockitoJUnitRunner.class)
public class DefaultDomainServiceTest {

    @InjectMocks
    DefaultDomainService defaultDomainService = new DefaultDomainService();
    @Mock
    DomainDao domainDao;

    DefaultDomainService spy;

    @Before
    public void setUp() throws Exception {
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
