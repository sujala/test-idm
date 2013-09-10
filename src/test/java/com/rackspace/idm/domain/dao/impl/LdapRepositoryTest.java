package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.audit.Audit;
import com.rackspace.test.SingleTestConfiguration;
import com.unboundid.ldap.sdk.*;
import com.unboundid.ldap.sdk.controls.SubtreeDeleteRequestControl;
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

import static junit.framework.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.mockito.Mockito.*;

/**
 * This class verifies the protected methods in the LdapRepository class.
 * <p/>
 * Groovy does not like Mockito and for this test I wanted to use
 * Mockito to verify that the correct/expected calls were made on the unboundid library.Groovy does not do
 * the verify calls right (would fail stating the expected calls were not made). For example:
 * <p/>
 * <p/>
 * <code>
 * lDAPInterface.delete(<any>);
 * -> at com.unboundid.ldap.sdk.LDAPInterface$delete$0.call(Unknown Source)
 * <p/>
 * However, there were other interactions with this mock:
 * -> at com.unboundid.ldap.sdk.LDAPInterface$delete.call(Unknown Source)
 * </code>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration()
public class LdapRepositoryTest {

    @Autowired
    LdapConnectionPools connPools;

    @Autowired
    LdapRepository ldapRepository;

    LDAPInterface ldapInterface = mock(LDAPInterface.class);

    @Before
    public void setup() {
        //reset the mock before each test
        reset(connPools);
        reset(ldapInterface);

        when(connPools.getAppConnPoolInterface()).thenReturn(ldapInterface);
    }

    @Test
    public void deleteEntryAndSubtree_SendsCriticalTreeDeleteRequest() throws Exception {
        ldapRepository.deleteEntryAndSubtree("asdf", mock(Audit.class));

        DeleteRequest actual = captureDeleteRequestFromDelete();
        Assert.assertNotNull(actual);

        Control control = actual.getControl(SubtreeDeleteRequestControl.SUBTREE_DELETE_REQUEST_OID);
        Assert.assertNotNull("Subtree delete control not added to request", control);
        Assert.assertTrue("Delete subtree control is not marked as critical", control.isCritical());
    }

    @Test
    public void deleteEntryAndSubtree_sendsCorrectBaseDNInDeleteRequest() throws Exception {
        String dn = "1234";
        ldapRepository.deleteEntryAndSubtree(dn, mock(Audit.class));

        DeleteRequest actual = captureDeleteRequestFromDelete();
        Assert.assertNotNull(actual);

        Assert.assertEquals(dn, actual.getDN());
    }

    @Test
    public void deleteEntryAndSubtree_exceptionIsWrappedAndAuditFails() throws Exception {
        when(ldapInterface.delete(any(DeleteRequest.class))).thenThrow(new LDAPException(ResultCode.UNWILLING_TO_PERFORM));
        Audit mockAudit = mock(Audit.class);
        try {
            ldapRepository.deleteEntryAndSubtree("blah", mockAudit);
            Assert.fail("Expected wrapped IllegalStateException to be thrown");
        }
        catch (IllegalStateException ex) {
            Assert.assertNotNull(ex.getCause());
            Assert.assertTrue(ex.getCause() instanceof LDAPException);
            Assert.assertEquals(ResultCode.UNWILLING_TO_PERFORM, ((LDAPException) ex.getCause()).getResultCode());
            verify(mockAudit).fail();
        }
        catch (Exception ex) {
            Assert.fail("Expected wrapped IllegalStateException to be thrown. Received '" + ex + "' instead.");
        }
    }

    @Test
    public void addEntry_CallsAddMethod() throws Exception {
        Audit audit = mock(Audit.class);
        String dn = "mydn";
        Attribute[] attributes = new Attribute[] {new Attribute("hello"), new Attribute("bye")};
        ldapRepository.addEntry(dn, attributes, audit);
        verify(ldapInterface).add(eq(dn), (Attribute)anyVararg());
    }

    @Test
    public void addEntry_exceptionIsWrappedAndAuditFails() throws Exception {
        Attribute[] atts = new Attribute[] {new Attribute("hi")};
        Audit mockAudit = mock(Audit.class);

        when(ldapInterface.add(anyString(), (Attribute)anyVararg())).thenThrow(new LDAPException(ResultCode.UNWILLING_TO_PERFORM));
        try {
            ldapRepository.addEntry("blah", atts, mockAudit);
            Assert.fail("Expected wrapped IllegalStateException to be thrown");
        }
        catch (IllegalStateException ex) {
            Assert.assertNotNull(ex.getCause());
            Assert.assertTrue(ex.getCause() instanceof LDAPException);
            Assert.assertEquals(ResultCode.UNWILLING_TO_PERFORM, ((LDAPException) ex.getCause()).getResultCode());
            verify(mockAudit).fail();
        }
        catch (Exception ex) {
            Assert.fail("Expected wrapped IllegalStateException to be thrown. Received '" + ex + "' instead.");
        }
    }

    @Test
    public void getAppInterface_callsConnectionPoolMethod() throws Exception {
        ldapRepository.getAppInterface();
        verify(connPools).getAppConnPoolInterface();
    }

    @Test
    public void getBindConnPool_callsConnectionPool() throws Exception {
        ldapRepository.getBindConnPool();
        verify(connPools).getBindConnPool();
    }

    private DeleteRequest captureDeleteRequestFromDelete() throws Exception {
        //capture the DeleteRequest sent
        ArgumentCaptor<DeleteRequest> model = ArgumentCaptor.forClass(DeleteRequest.class);
        verify(ldapInterface).delete(model.capture());

        DeleteRequest actual = model.getValue();
        Assert.assertNotNull(actual);
        return actual;
    }

    private DeleteRequest captureAddRequestFromDelete() throws Exception {
        //capture the DeleteRequest sent
        ArgumentCaptor<DeleteRequest> model = ArgumentCaptor.forClass(DeleteRequest.class);
        verify(ldapInterface).delete(model.capture());

        DeleteRequest actual = model.getValue();
        Assert.assertNotNull(actual);
        return actual;
    }


    /**
     * This config is used to wire the dependencies into the LdapEndpointRepository. The context file
     * LdapEndpointRepositoryTest-context.xml
     * loads this file.
     */
    @SingleTestConfiguration
    public static class Config {
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

        @Bean
        LdapRepository testingLdapRepository() {
            return new LdapRepository() {

            };
        }
    }

}
