package com.rackspace.idm.domain.dao.impl;

import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPInterface;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static junit.framework.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * * These tests should be migrated to LdapRepositoryTest and LdapEndpointRepositoryTest depending on whether the test
 * is testing a method
 * in LdapRepository or LdapEndpointRepository.
 * <p>
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 7/3/12
 * Time: 12:21 PM
 * To change this template use File | Settings | File Templates.
 */
@RunWith(MockitoJUnitRunner.class)
public class LdapEndpointRepositoryDepTest extends InMemoryLdapIntegrationTest {
    @InjectMocks
    LdapEndpointRepository ldapEndpointRepository = new LdapEndpointRepository();
    LDAPInterface ldapInterface;
    @Mock
    LdapConnectionPools connPools = mock(LdapConnectionPools.class);
    @Mock
    Configuration config = mock(Configuration.class);

    @Before
    public void setUp() throws Exception {
        ldapInterface = mock(LDAPInterface.class);
    }

    @Test
    public void getLdapPagingOffsetDefault_callsConfigMethod() throws Exception {
        ldapEndpointRepository.getLdapPagingOffsetDefault();
        verify(config).getInt("ldap.paging.offset.default");
    }

    @Test
    public void getLdapPagingLimitDefault_callsConfigMethod() throws Exception {
        ldapEndpointRepository.getLdapPagingLimitDefault();
        verify(config).getInt("ldap.paging.limit.default");
    }

    @Test
    public void getRackspaceInumPrefix_callsConfigMethod() throws Exception {
        ldapEndpointRepository.getRackspaceInumPrefix();
        verify(config).getString("rackspace.inum.prefix");
    }

    @Test
    public void queryPairConstructor_attributeIsBlank_throwsIllegalArgumentException() throws Exception {
        try {
            new LdapRepository.QueryPair("", "comparer", "value");
            assertTrue("should throw exception", false);
        } catch (IllegalArgumentException ex) {
            assertThat("has correct message", ex.getMessage(), equalTo("attribute cannot be empty"));
        }
    }

    @Test
    public void queryPairConstructor_comparerIsBlank_throwsIllegalArgumentException() throws Exception {
        try {
            new LdapRepository.QueryPair("attribute", "", "value");
            assertTrue("should throw exception", false);
        } catch (IllegalArgumentException ex) {
            assertThat("has correct message", ex.getMessage(), equalTo("comparer cannot be empty"));
        }
    }

    @Test
    public void queryPairConstructor_valueIsBlank_throwsIllegalArgumentException() throws Exception {
        try {
            new LdapRepository.QueryPair("attribute", "comparer", "");
            assertTrue("should throw exception", false);
        } catch (IllegalArgumentException ex) {
            assertThat("has correct message", ex.getMessage(), equalTo("value cannot be empty"));
        }
    }

    @Test
    public void addEqualAttribute_addsFilterToList() throws Exception {
        byte[] bytes = "attributeValue".getBytes();
        LdapRepository.LdapSearchBuilder searchBuilder = new LdapRepository.LdapSearchBuilder();
        LdapRepository.LdapSearchBuilder returnedValue = searchBuilder.addEqualAttribute("attributeName", bytes);
        assertThat("search builder has filter with correct attribute name", returnedValue.build().getAttributeName(), equalTo("attributeName"));
        assertThat("search builder has filter with correct attribute value", returnedValue.build().getAssertionValue(), equalTo("attributeValue"));
    }

    @Test
    public void addGreaterOrEqualAttribute_addsFilterToList() throws Exception {
        LdapRepository.LdapSearchBuilder searchBuilder = new LdapRepository.LdapSearchBuilder();
        LdapRepository.LdapSearchBuilder returnedValue = searchBuilder.addGreaterOrEqualAttribute("attributeName", "attributeValue");
        assertThat("search builder has filter with correct attribute name", returnedValue.build().getAttributeName(), equalTo("attributeName"));
        assertThat("search builder has filter with correct attribute value", returnedValue.build().getAssertionValue(), equalTo("attributeValue"));
    }

    @Test
    public void build_filterListEmpty_returnsNewFilter() throws Exception {
        LdapRepository.LdapSearchBuilder searchBuilder = new LdapRepository.LdapSearchBuilder();
        Filter filter = searchBuilder.build();
        assertThat("returned filter attribute name", filter.getAttributeName(), equalTo("objectClass"));
        assertThat("returned filter attribute value", filter.getAssertionValue(), equalTo("*"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void addBaseUrl_baseUrlIsNull_throwsIllegalArgumentException() throws Exception {
        ldapEndpointRepository.addBaseUrl(null);
    }

}
