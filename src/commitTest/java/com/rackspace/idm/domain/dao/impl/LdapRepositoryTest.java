package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.exception.IdmException;
import com.rackspace.test.SingleTestConfiguration;
import com.unboundid.ldap.sdk.*;
import com.unboundid.ldap.sdk.controls.SubtreeDeleteRequestControl;
import junit.framework.Assert;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Rule;
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

    @Autowired
    Configuration config;

    LDAPInterface ldapInterface = mock(LDAPInterface.class);

    @Before
    public void setup() {
        //reset the mock before each test
        reset(connPools);
        reset(ldapInterface);
        config.clear();

        when(connPools.getAppConnPoolInterface()).thenReturn(ldapInterface);
    }

    /**
     * The choice of the algorithm for deleting the subtree depends on the flag feature.use.subtree.delete.control.for.subtree.deletion.enabled.
     * When set to false, the recursion algorithm is chosen. Can determine this recursion algorithm is chosen because it
     * will immediately perform a search to retrieve the specified dn
     */
    @Test
    public void deleteEntryAndSubtree_chooseRecursionAlgorithmWhenFlagFalse () throws Exception {
        String errorMessage = "Search called";
        String testDn = "asdf";

        config.setProperty(LdapRepository.FEATURE_USE_SUBTREE_DELETE_CONTROL_FOR_SUBTREE_DELETION_PROPNAME, false);

        // the test is just ensuring the right algorithm is chosen based on the flag. The recursion algorithm will call this
        // method (while the subtreecontrol won't). So if this method is called, we know the right algorithm was chosen.
        // Shortcircuit the rest of the method execution by having the mock throw an exception since at this point the purpose of the test
        // has been verified. Not verifying the correct operation of the recursion algorithm in this test.
        when(ldapInterface.search(anyString(), any(SearchScope.class), anyString(), anyString())).thenThrow(new IdmException("Search called"));

        try {
            ldapRepository.deleteEntryAndSubtree(testDn, mock(Audit.class));
        } catch (IdmException e) {
            Assert.assertEquals("Search not called as part of recursion algorithm", errorMessage, e.getMessage());
        }

        //verify the search call was actually made since the test depends on this determining whether the correct algorithm was chosen
        verify(ldapInterface).search(eq(testDn), eq(SearchScope.ONE), anyString(), anyString());
    }

    /**
     * The choice of the algorithm for deleting the subtree depends on the flag feature.use.subtree.delete.control.for.subtree.deletion.enabled.
     * When set to true, the subtreedeletecontrol algorithm is chosen. Can determine this algorithm is chosen by verifying
     * the control was added to the delete request sent to the unboundid library.
     */
    @Test
    public void deleteEntryAndSubtree_chooseSubtreeDeleteControlAlgorithmWhenFlagFalse () throws Exception {
        config.setProperty(LdapRepository.FEATURE_USE_SUBTREE_DELETE_CONTROL_FOR_SUBTREE_DELETION_PROPNAME, true);

        ldapRepository.deleteEntryAndSubtree("asdf", mock(Audit.class));

        DeleteRequest actual = captureDeleteRequestFromDelete();
        Assert.assertNotNull(actual);

        Control control = actual.getControl(SubtreeDeleteRequestControl.SUBTREE_DELETE_REQUEST_OID);
        Assert.assertNotNull("Subtree delete control not added to request", control);
        Assert.assertTrue("Delete subtree control is not marked as critical", control.isCritical());
    }

    @Test
    public void deleteEntryAndSubtreeUsingSubtreeControl_SendsCriticalTreeDeleteRequest() throws Exception {
        ldapRepository.deleteEntryAndSubtreeUsingSubtreeDeleteControl("asdf", mock(Audit.class));

        DeleteRequest actual = captureDeleteRequestFromDelete();
        Assert.assertNotNull(actual);

        Control control = actual.getControl(SubtreeDeleteRequestControl.SUBTREE_DELETE_REQUEST_OID);
        Assert.assertNotNull("Subtree delete control not added to request", control);
        Assert.assertTrue("Delete subtree control is not marked as critical", control.isCritical());
    }

    @Test
    public void deleteEntryAndSubtreeUsingSubtreeControl_sendsCorrectBaseDNInDeleteRequest() throws Exception {
        String dn = "1234";
        ldapRepository.deleteEntryAndSubtreeUsingSubtreeDeleteControl(dn, mock(Audit.class));

        DeleteRequest actual = captureDeleteRequestFromDelete();
        Assert.assertNotNull(actual);

        Assert.assertEquals(dn, actual.getDN());
    }

    @Test
    public void deleteEntryAndSubtreeUsingSubtreeControl_exceptionIsWrappedAndAuditFails() throws Exception {
        when(ldapInterface.delete(any(DeleteRequest.class))).thenThrow(new LDAPException(ResultCode.UNWILLING_TO_PERFORM));
        Audit mockAudit = mock(Audit.class);
        try {
            ldapRepository.deleteEntryAndSubtreeUsingSubtreeDeleteControl("blah", mockAudit);
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
