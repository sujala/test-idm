package com.rackspace.idm.domain.entity;

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
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
public class DomainTest {
    Domain domain;

    @Before
    public void setUp() throws Exception {
        domain = new Domain();
    }

    @Test
    public void getLdapEntry_returnsLdapEntry() throws Exception {
        ReadOnlyEntry readOnlyEntry = new ReadOnlyEntry("uniqueId", new Attribute[0]);
        domain.setLdapEntry(readOnlyEntry);
        ReadOnlyEntry result = domain.getLDAPEntry();
        assertThat("ldap entry", result, equalTo(readOnlyEntry));
    }

    @Test
    public void addTenantId_tenantId_doesNotRemoveId() throws Exception {
        String[] test = {"123"};
        domain.setTenantIds(test);
        String[] result = domain.getTenantIds();
        assertThat("tenant id", result[0], equalTo("123"));
        assertThat("string list", result.length, equalTo(1));
    }
}
