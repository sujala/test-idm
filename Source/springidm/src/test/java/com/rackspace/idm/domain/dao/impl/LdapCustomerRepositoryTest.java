package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.entity.Customer;
import com.unboundid.ldap.sdk.*;
import org.apache.commons.configuration.Configuration;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 4/25/12
 * Time: 10:55 AM
 */
public class LdapCustomerRepositoryTest {

    LdapCustomerRepository ldapCustomerRepository;
    LdapCustomerRepository spy;
    LDAPInterface ldapInterface;

    @Before
    public void setUp() throws Exception {
        ldapCustomerRepository = new LdapCustomerRepository(mock(LdapConnectionPools.class),mock(Configuration.class));
        ldapInterface = mock(LDAPInterface.class);
        spy = spy(ldapCustomerRepository);

        doReturn(ldapInterface).when(spy).getAppInterface();
    }

    @Test
    public void getCustomerByCustomerId_withNullId_returnsNull() throws Exception {
        ldapCustomerRepository.getCustomerByCustomerId(null);
    }

    @Test
    public void getCustomerByCustomerId_withEmptyString_returnsNull() throws Exception {
        ldapCustomerRepository.getCustomerByCustomerId("");
    }

    @Test
    public void getCustomerByCustomerId_foundCustomer_returnsCustomer() throws Exception {
        Customer customer = new Customer();
        SearchResultEntry searchResultEntry = new SearchResultEntry("", new Attribute[0]);
        doReturn(searchResultEntry).when(spy).getSingleEntry(anyString(), any(SearchScope.class), any(Filter.class));
        doReturn(customer).when(spy).getCustomer(searchResultEntry);
        Customer result = spy.getCustomerByCustomerId("customerId");
        assertThat("customer", result, equalTo(customer));
    }

    @Test
    public void getCustomerByCustomerId_customerNotFound_returnsNull() throws Exception {
        doReturn(null).when(spy).getSingleEntry(anyString(), any(SearchScope.class), any(Filter.class));
        Customer result = spy.getCustomerByCustomerId("customerId");
        assertThat("customer", result, equalTo(null));
    }

    @Test (expected = IllegalArgumentException.class)
    public void addCustomer_customerIsNull_throwsIllegalArgument() throws Exception {
        ldapCustomerRepository.addCustomer(null);
    }

    @Test
    public void addCustomer_callsAddEntry() throws Exception {
        Customer customer = new Customer();
        customer.setId("id");
        doNothing().when(spy).addEntry(anyString(), any(Attribute[].class), any(Audit.class));
        spy.addCustomer(customer);
        verify(spy).addEntry(anyString(), any(Attribute[].class), any(Audit.class));
    }

    @Test (expected = IllegalArgumentException.class)
    public void deleteCustomer_customerIdIsBlank_throwsIllegalArgument() throws Exception {
        ldapCustomerRepository.deleteCustomer("");
    }

    @Test
    public void deleteCustomer_callsDeleteEntryAndSubtree() throws Exception {
        Customer customer = new Customer();
        customer.setUniqueId("uniqueId");
        doReturn(customer).when(spy).getCustomerByCustomerId("customerId");
        doNothing().when(spy).deleteEntryAndSubtree(anyString(), any(Audit.class));
        spy.deleteCustomer("customerId");
        verify(spy).deleteEntryAndSubtree(eq("uniqueId"), any(Audit.class));
    }

    @Test
    public void getAllCustomers_addCustomerToList_returnsCustomerList() throws Exception {
        Customer customer = new Customer();
        SearchResultEntry searchResultEntry = new SearchResultEntry("", new Attribute[0]);
        List<SearchResultEntry> entries = new ArrayList<SearchResultEntry>();
        entries.add(searchResultEntry);
        doReturn(entries).when(spy).getMultipleEntries(anyString(), any(SearchScope.class), any(Filter.class), anyString());
        doReturn(customer).when(spy).getCustomer(searchResultEntry);
        List<Customer> result = spy.getAllCustomers();
        assertThat("customer", result.get(0), equalTo(customer));
    }

    @Test
    public void getAllCustomers_noEntries_returnsEmptyCustomerList() throws Exception {
        List<SearchResultEntry> entries = new ArrayList<SearchResultEntry>();
        doReturn(entries).when(spy).getMultipleEntries(anyString(), any(SearchScope.class), any(Filter.class), anyString());
        List<Customer> result = spy.getAllCustomers();
        assertThat("empty list", result.isEmpty(), equalTo(true));
    }

    @Test (expected = IllegalArgumentException.class)
    public void getCustomerById_idIsBlank_throwsIllegalArgument() throws Exception {
        ldapCustomerRepository.getCustomerById("");
    }

    @Test
    public void getCustomerById_foundCustomer_returnsCustomer() throws Exception {
        Customer customer = new Customer();
        SearchResultEntry searchResultEntry = new SearchResultEntry("", new Attribute[0]);
        doReturn(searchResultEntry).when(spy).getSingleEntry(anyString(), any(SearchScope.class), any(Filter.class));
        doReturn(customer).when(spy).getCustomer(searchResultEntry);
        Customer result = spy.getCustomerById("id");
        assertThat("customer", result, equalTo(customer));
    }

    @Test
    public void getCustomerById_customerNotFound_returnsNull() throws Exception {
        doReturn(null).when(spy).getSingleEntry(anyString(), any(SearchScope.class), any(Filter.class));
        Customer result = spy.getCustomerById("id");
        assertThat("customer", result, equalTo(null));
    }

    @Test (expected = IllegalArgumentException.class)
    public void updateCustomer_customerIsNull_throwsIllegalArgument() throws Exception {
        ldapCustomerRepository.updateCustomer(null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void updateCustomer_customerRCNIsBlank_throwsIllegalArgument() throws Exception {
        ldapCustomerRepository.updateCustomer(new Customer());
    }

    @Test (expected = IllegalArgumentException.class)
    public void updateCustomer_customerNotFound_throwsIllegalArgument() throws Exception {
        Customer customer = new Customer();
        customer.setRCN("rcn");
        doReturn(null).when(spy).getCustomerByCustomerId("rcn");
        spy.updateCustomer(customer);
    }

    @Test
    public void updateCustomer_noModification_returns()throws Exception {
        Customer customer = new Customer();
        customer.setRCN("rcn");
        doReturn(customer).when(spy).getCustomerByCustomerId("rcn");
        doReturn(new ArrayList<Modification>()).when(spy).getModifications(customer, customer);
        spy.updateCustomer(customer);
        verify(spy).getModifications(customer, customer);
    }

    @Test
    public void updateCustomer_callsUpdateEntry() throws Exception {
        Customer customer = new Customer();
        customer.setRCN("rcn");
        Modification modification = new Modification(ModificationType.REPLACE, "replace");
        doReturn(customer).when(spy).getCustomerByCustomerId("rcn");
        ArrayList<Modification> mods = new ArrayList<Modification>();
        mods.add(modification);
        doReturn(mods).when(spy).getModifications(customer, customer);
        doNothing().when(spy).updateEntry(anyString(), eq(mods), any(Audit.class));
        spy.updateCustomer(customer);
        verify(spy).updateEntry(anyString(), eq(mods), any(Audit.class));
    }

    @Test
    public void getAddAttributes_addsAllAttributes_returnsArray() throws Exception {
        Customer customer = new Customer();
        customer.setId("id");
        customer.setRCN("rcn");
        customer.setEnabled(true);
        Attribute[] result = ldapCustomerRepository.getAddAttributes(customer);
        assertThat("id", result[1].getValue(), equalTo("id"));
        assertThat("rcn", result[2].getValue(), equalTo("rcn"));
        assertThat("enabled", result[3].getValue(), equalTo("true"));
    }

    @Test
    public void getAddAttributes_noAttributesAdded_returnsListWithNoAtts() throws Exception {
        Customer customer = new Customer();
        Attribute[] result = ldapCustomerRepository.getAddAttributes(customer);
        assertThat("attribute list", result.length, equalTo(1));
    }

    @Test
    public void getCustomer_setupCustomerAttributes_returnsCustomer() throws Exception {
        Attribute[] attributes = new Attribute[4];
        attributes[0] = new Attribute(LdapRepository.ATTR_RACKSPACE_CUSTOMER_NUMBER, "rcn");
        attributes[1] = new Attribute(LdapRepository.ATTR_PASSWORD_ROTATION_DURATION, "1");
        attributes[2] = new Attribute(LdapRepository.ATTR_PASSWORD_ROTATION_ENABLED, "true");
        attributes[3] = new Attribute(LdapRepository.ATTR_ENABLED, "true");
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId",attributes);
        Customer result = ldapCustomerRepository.getCustomer(searchResultEntry);
        assertThat("rcn", result.getRCN(), equalTo("rcn"));
        assertThat("password rotation duration", result.getPasswordRotationDuration(), equalTo(1));
        assertThat("password rotation enabled", result.getPasswordRotationEnabled(), equalTo(true));
        assertThat("enabled", result.isEnabled(), equalTo(true));
    }

    @Test
    public void getModifications_enablednotNullAndNotEqual_addsMod() throws Exception {
        Customer cNew = new Customer();
        Customer cOld = new Customer();
        cNew.setEnabled(true);
        cOld.setEnabled(false);
        List<Modification> result = ldapCustomerRepository.getModifications(cOld, cNew);
        assertThat("mod", result.get(0).getModificationType().toString(), equalTo("REPLACE"));
        assertThat("attribute", result.get(0).getAttributeName(), equalTo(LdapRepository.ATTR_ENABLED));
        assertThat("list length", result.size(), equalTo(1));
    }

    @Test
    public void getModifications_passwordRotationEnabledNotNullAndNotEqual_addsMod() throws Exception {
        Customer cNew = new Customer();
        Customer cOld = new Customer();
        cNew.setEnabled(true);
        cOld.setEnabled(true);
        cNew.setPasswordRotationEnabled(true);
        cOld.setPasswordRotationEnabled(false);
        List<Modification> result = ldapCustomerRepository.getModifications(cOld, cNew);
        assertThat("mod", result.get(0).getModificationType().toString(), equalTo("REPLACE"));
        assertThat("attribute", result.get(0).getAttributeName(), equalTo(LdapRepository.ATTR_PASSWORD_ROTATION_ENABLED));
        assertThat("list length", result.size(), equalTo(1));
    }

    @Test
    public void getModifications_passwordRotationDurationNotEqual_addsMod() throws Exception {
        Customer cNew = new Customer();
        Customer cOld = new Customer();
        cOld.setEnabled(true);
        cNew.setPasswordRotationEnabled(true);
        cOld.setPasswordRotationEnabled(true);
        cNew.setPasswordRotationDuration(1);
        cOld.setPasswordRotationDuration(2);
        List<Modification> result = ldapCustomerRepository.getModifications(cOld, cNew);
        assertThat("mod", result.get(0).getModificationType().toString(), equalTo("REPLACE"));
        assertThat("attribute", result.get(0).getAttributeName(), equalTo(LdapRepository.ATTR_PASSWORD_ROTATION_DURATION));
        assertThat("list length", result.size(), equalTo(1));
    }

    @Test
    public void getModifications_noModifications_doesNotAddMod() throws Exception {
        Customer cNew = new Customer();
        Customer cOld = new Customer();
        cNew.setPasswordRotationDuration(1);
        cOld.setPasswordRotationDuration(1);
        List<Modification> result = ldapCustomerRepository.getModifications(cOld, cNew);
        assertThat("list length", result.isEmpty(), equalTo(true));
    }

    @Test
    public void getNextCustomerId_callsGetNextId() throws Exception {
        doReturn("").when(spy).getNextId(LdapRepository.NEXT_CUSTOMER_ID);
        spy.getNextCustomerId();
        verify(spy).getNextId(LdapRepository.NEXT_CUSTOMER_ID);
    }

    @Test (expected = IllegalStateException.class)
    public void softDeleteCustomer_callsLDAPInterfaceModifyDN_throwsLDAPException() throws Exception {
        Customer customer = new Customer();
        customer.setUniqueId("uniqueId");
        customer.setId("id");
        doThrow(new LDAPException(ResultCode.LOCAL_ERROR)).when(ldapInterface).modifyDN(anyString(), anyString(), anyBoolean(), anyString());
        spy.softDeleteCustomer(customer);
    }

    @Test
    public void softDeleteCustomer_callsLDAPInterface_modify() throws Exception {
        Customer customer = new Customer();
        customer.setUniqueId("uniqueId");
        customer.setId("id");
        when(ldapInterface.modifyDN(anyString(), anyString(), anyBoolean(), anyString())).thenReturn(new LDAPResult(1, ResultCode.SUCCESS));
        when(ldapInterface.modify(anyString(), any(Modification.class))).thenReturn(new LDAPResult(1, ResultCode.SUCCESS));
        spy.softDeleteCustomer(customer);
        verify(ldapInterface).modify(anyString(), any(Modification.class));
    }

    @Test (expected = IllegalArgumentException.class)
    public void getSoftDeletedCustomerById_idIsBlank_throwsIllegalArgument() throws Exception {
        ldapCustomerRepository.getSoftDeletedCustomerById("");
    }

    @Test
    public void getSoftDeletedCustomerById_foundCustomer_returnsCustomer() throws Exception {
        Customer customer = new Customer();
        SearchResultEntry searchResultEntry = new SearchResultEntry("", new Attribute[0]);
        doReturn(searchResultEntry).when(spy).getSingleEntry(anyString(), any(SearchScope.class), any(Filter.class));
        doReturn(customer).when(spy).getCustomer(searchResultEntry);
        Customer result = spy.getSoftDeletedCustomerById("id");
        assertThat("customer", result, equalTo(customer));
    }

    @Test
    public void getSoftDeletedCustomerById_customerNotFound_returnsNull() throws Exception {
        doReturn(null).when(spy).getSingleEntry(anyString(), any(SearchScope.class), any(Filter.class));
        Customer result = spy.getSoftDeletedCustomerById("id");
        assertThat("customer", result, equalTo(null));
    }

    @Test (expected = IllegalArgumentException.class)
    public void getSoftDeletedUserByCustomerId_customerIdIsBlank_throwsIllegalArgument() throws Exception {
        ldapCustomerRepository.getSoftDeletedUserByCustomerId("");
    }

    @Test
    public void getSoftDeletedUserByCustomerId_foundCustomer_returnsCustomer() throws Exception {
        Customer customer = new Customer();
        SearchResultEntry searchResultEntry = new SearchResultEntry("", new Attribute[0]);
        doReturn(searchResultEntry).when(spy).getSingleEntry(anyString(), any(SearchScope.class), any(Filter.class));
        doReturn(customer).when(spy).getCustomer(searchResultEntry);
        Customer result = spy.getSoftDeletedUserByCustomerId("customerId");
        assertThat("customer", result, equalTo(customer));
    }

    @Test
    public void getSoftDeletedUserByCustomerId_customerNotFound_returnsNull() throws Exception {
        doReturn(null).when(spy).getSingleEntry(anyString(), any(SearchScope.class), any(Filter.class));
        Customer result = spy.getSoftDeletedUserByCustomerId("customerId");
        assertThat("customer", result, equalTo(null));
    }

    @Test (expected = IllegalStateException.class)
    public void unSoftDeleteCustomer_callsLDAPInterfaceModifyDN_throwsLDAPException() throws Exception {
        Customer customer = new Customer();
        customer.setUniqueId("uniqueId");
        customer.setId("id");
        doThrow(new LDAPException(ResultCode.LOCAL_ERROR)).when(ldapInterface).modifyDN(anyString(), anyString(), anyBoolean(), anyString());
        spy.unSoftDeleteCustomer(customer);
    }

    @Test
    public void unSoftDeleteCustomer_callsLDAPInterface_modify() throws Exception {
        Customer customer = new Customer();
        customer.setUniqueId("uniqueId");
        customer.setId("id");
        when(ldapInterface.modifyDN(anyString(), anyString(), anyBoolean(), anyString())).thenReturn(new LDAPResult(1, ResultCode.SUCCESS));
        when(ldapInterface.modify(anyString(), any(Modification.class))).thenReturn(new LDAPResult(1, ResultCode.SUCCESS));
        spy.unSoftDeleteCustomer(customer);
        verify(ldapInterface).modify(anyString(), any(Modification.class));
    }
}
