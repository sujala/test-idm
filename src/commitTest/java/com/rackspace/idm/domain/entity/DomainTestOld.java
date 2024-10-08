package com.rackspace.idm.domain.entity;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Created by IntelliJ IDEA.
 * User: matt.colton
 * Date: 8/13/12
 * Time: 9:51 AM
 * To change this template use File | Settings | File Templates.
 */

public class DomainTestOld {
    Domain domain;

    @Before
    public void setUp() throws Exception {
        domain = new Domain();
    }

    @Test
    public void getDomain_returnsDomain() throws Exception {
        String[] tenants = {"123"};
        domain.setDomainId("1");
        domain.setName("name");
        domain.setDescription("description");
        domain.setEnabled(true);
        domain.setTenantIds(tenants);
        assertThat("id", domain.getDomainId(), equalTo("1"));
        assertThat("name", domain.getName(), equalTo("name"));
        assertThat("description", domain.getDescription(), equalTo("description"));
        assertThat("enabled", domain.getEnabled(), equalTo(true));
    }

    @Test
    public void addTenantId_tenantId_doesNotRemoveId() throws Exception {
        String[] tenants = {"123"};
        domain.setTenantIds(tenants);
        String[] result = domain.getTenantIds();
        assertThat("tenant id", result[0], equalTo("123"));
        assertThat("string list", result.length, equalTo(1));
    }
}
