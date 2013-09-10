package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.test.SingleTestConfiguration;
import com.unboundid.ldap.sdk.*;
import junit.framework.Assert;
import org.apache.commons.configuration.BaseConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


import static org.mockito.Mockito.*;

/**
 * This class is a new version of the LdapEndpointRepositoryTestOld. Groovy does not like Mockito and for this test I wanted to use
 * Mockito to verify that the correct/expected calls were made on the unboundid library. When I copied this code to groovy I could
 * not get the verify calls to work (would failing stating the expected calls were not made). For example:
 * <p>
 *
 * <code>
 * lDAPInterface.delete(<any>);
 -> at com.unboundid.ldap.sdk.LDAPInterface$delete$0.call(Unknown Source)

 However, there were other interactions with this mock:
 -> at com.unboundid.ldap.sdk.LDAPInterface$delete.call(Unknown Source)
 </code>
 *
 * The test class uses spring to facilitiate wiring of the mocks within the LdapEndpointRepository since the class does not have setters for the dependencies
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration()
public class LdapEndpointRepositoryTest {

    @Autowired LdapEndpointRepository repo;
    @Autowired LdapConnectionPools connPools;

    LDAPInterface ldapInterface = mock(LDAPInterface.class);

    @Before
    public void setup() {
        //reset the mock before each test
        reset(repo);
        reset(connPools);
        reset(ldapInterface);
        when(connPools.getAppConnPoolInterface()).thenReturn(ldapInterface);
    }

    @Test
    public void deleteBaseUrl_SendsAppropriateFilterToDelete() throws Exception {
        String baseUrlID = "1";
        doNothing().when(repo).deleteObject(any(Filter.class));

        repo.deleteBaseUrl(baseUrlID);

        ArgumentCaptor<Filter> filterArg = ArgumentCaptor.forClass(Filter.class);
        verify(repo).deleteObject(filterArg.capture());

        Filter filter = filterArg.getValue();
        Assert.assertEquals("(&(rsid=1)(objectclass=baseurl))", filter.toNormalizedString());
    }

    /**
     * This config is used to wire the dependencies into the LdapEndpointRepository. The context file LdapEndpointRepositoryTest-context.xml
     * loads this file.
     */
    @SingleTestConfiguration
    public static class Config {
        @Bean
        LdapEndpointRepository ldapEndpointRepository() {
            //use a spy because want to mock superclass methods
            return spy(new LdapEndpointRepository());
        }

        @Bean
        LdapPaginatorRepository ldapPaginatorRepository() {
            return mock(LdapPaginatorRepository.class);
        }

        @Bean
        LdapConnectionPools connPools() {
            LdapConnectionPools connPools = mock(LdapConnectionPools.class);
            return connPools;
        }

        @Bean
        org.apache.commons.configuration.Configuration configuration() {
            BaseConfiguration config = new BaseConfiguration();
            return config;
        }

    }
}
