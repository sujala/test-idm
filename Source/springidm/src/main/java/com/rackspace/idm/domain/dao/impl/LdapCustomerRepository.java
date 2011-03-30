package com.rackspace.idm.domain.dao.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.dao.CustomerDao;
import com.rackspace.idm.domain.entity.Customer;
import com.rackspace.idm.domain.entity.CustomerStatus;
import com.rackspace.idm.util.InumHelper;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.DeleteRequest;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPResult;
import com.unboundid.ldap.sdk.LDAPSearchException;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldap.sdk.controls.SubtreeDeleteRequestControl;

public class LdapCustomerRepository extends LdapRepository implements
    CustomerDao {

    public LdapCustomerRepository(LdapConnectionPools connPools,
        Configuration config) {
        super(connPools, config);
    }

    public void add(Customer customer) {
        getLogger().info("Adding customer {}", customer);
        if (customer == null) {
            getLogger().error("Null instance of Customer was passed.");
            throw new IllegalArgumentException(
                "Null instance of Customer was passed.");
        }

        List<Attribute> atts = new ArrayList<Attribute>();

        atts.add(new Attribute(ATTR_OBJECT_CLASS,
            ATTR_CUSTOMER_OBJECT_CLASS_VALUES));

        if (!StringUtils.isBlank(customer.getIname())) {
            atts.add(new Attribute(ATTR_INAME, customer.getIname()));
        }

        if (!StringUtils.isBlank(customer.getInum())) {
            atts.add(new Attribute(ATTR_INUM, customer.getInum()));
            atts.add(new Attribute(ATTR_O, customer.getInum()));
        }

        if (!StringUtils.isBlank(customer.getCustomerId())) {
            atts.add(new Attribute(ATTR_RACKSPACE_CUSTOMER_NUMBER, customer
                .getCustomerId()));
        }

        if (customer.getStatus() != null) {
            atts.add(new Attribute(ATTR_STATUS, customer.getStatus().toString()));
        }

        if (customer.isLocked() != null) {
            atts.add(new Attribute(ATTR_LOCKED, String.valueOf(customer
                .isLocked())));
        }

        if (customer.getSoftDeleted() != null) {
            atts.add(new Attribute(ATTR_SOFT_DELETED, String.valueOf(customer
                .getSoftDeleted())));
        }

        Attribute[] attributes = atts.toArray(new Attribute[0]);

        LDAPResult result;

        String customerDN = new LdapDnBuilder(BASE_DN).addAttriubte(ATTR_O,
            customer.getInum()).build();

        customer.setUniqueId(customerDN);

        String customerGroupsDN = new LdapDnBuilder(customerDN).addAttriubte(
            ATTR_OU, OU_GROUPS_NAME).build();
        String customerPeopleDN = new LdapDnBuilder(customerDN).addAttriubte(
            ATTR_OU, OU_PEOPLE_NAME).build();
        String customerApplicationsDN = new LdapDnBuilder(customerDN)
            .addAttriubte(ATTR_OU, OU_APPLICATIONS_NAME).build();

        LDAPConnection conn = null;
        Audit audit = Audit.log(customer).add();
        try {
            conn = getAppConnPool().getConnection();
            result = conn.add(customerDN, attributes);
        } catch (LDAPException ldapEx) {
        	audit.fail();
            getLogger()
                .error("Error adding customer {} - {}", customer, ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            audit.fail();
            getLogger().error("Error adding customerId {} - {}",
                customer.getCustomerId(), result.getResultCode());
            throw new IllegalStateException(String.format(
                "LDAP error encountered when adding customerId: %s - %s",
                customer.getCustomerId(), result.getResultCode().toString()));
        }

        // Add ou=groups under new customer entry
        Attribute[] groupAttributes = {
            new Attribute(ATTR_OBJECT_CLASS, ATTR_OBJECT_CLASS_OU_VALUES),
            new Attribute(ATTR_OU, OU_GROUPS_NAME)};

        try {
            result = conn.add(customerGroupsDN, groupAttributes);
        } catch (LDAPException ldapEx) {
            audit.fail();
            getLogger().error("Error adding customer groups {} - {}", customer,
                ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            audit.fail();
            getLogger().error(
                "Error adding customer groups for customerId {} - {}",
                customer.getCustomerId(), result.getResultCode());
            throw new IllegalStateException(
                String
                    .format(
                        "LDAP error encountered when adding customer groups for customerId: %s - %s",
                        customer.getCustomerId(), result.getResultCode()
                            .toString()));
        }

        // Add ou=people under new customer entry
        Attribute[] peopleAttributes = {
            new Attribute(ATTR_OBJECT_CLASS, ATTR_OBJECT_CLASS_OU_VALUES),
            new Attribute(ATTR_OU, OU_PEOPLE_NAME)};

        try {
            result = conn.add(customerPeopleDN, peopleAttributes);
        } catch (LDAPException ldapEx) {
            audit.fail();
            getLogger().error("Error adding customer people {} - {}", customer,
                ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            audit.fail();
            getLogger().error(
                "Error adding customer people for customerId {} - {}",
                customer.getCustomerId(), result.getResultCode());
            throw new IllegalStateException(
                String
                    .format(
                        "LDAP error encountered when adding customer people for customerId: %s - %s",
                        customer.getCustomerId(), result.getResultCode()
                            .toString()));
        }

        // Add ou=applications under new customer entry
        Attribute[] applicationAttributes = {
            new Attribute(ATTR_OBJECT_CLASS, ATTR_OBJECT_CLASS_OU_VALUES),
            new Attribute(ATTR_OU, "applications")};

        try {
            result = conn.add(customerApplicationsDN,
                applicationAttributes);
        } catch (LDAPException ldapEx) {
            audit.fail();
            getLogger().error("Error adding customer applications {} - {}",
                customer, ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            audit.fail();
            getLogger().error(
                "Error adding customer applications for customerId {} - {}",
                customer.getCustomerId(), result.getResultCode());
            throw new IllegalStateException(
                String
                    .format(
                        "LDAP error encountered when adding customer applications for customerId: %s - %s",
                        customer.getCustomerId(), result.getResultCode()
                            .toString()));
        }
        audit.succeed();
        
        getAppConnPool().releaseConnection(conn);

        getLogger().debug("Added customer {}", customer);
    }

    public void delete(String customerId) {
        getLogger().info("Deleting customer {}", customerId);
        if (StringUtils.isBlank(customerId)) {
            getLogger().error("Null or Empty customerId paramter");
            throw new IllegalArgumentException(
                "Null or Empty customerId parameter.");
        }
        
        Customer customer = findByCustomerId(customerId);

        String customerDN = customer.getUniqueId();
        
        Audit audit = Audit.log(customer).delete();

        LDAPResult result = null;
        
        try {
            String ouGroupsDn = new LdapDnBuilder(customerDN).addAttriubte(
                ATTR_OU, OU_GROUPS_NAME).build();
            DeleteRequest request = new DeleteRequest(ouGroupsDn);
            result = getAppConnPool().delete(request);
        } catch (LDAPException ldapEx) {
            audit.fail(ldapEx.getExceptionMessage());
            getLogger().error("Could not perform delete for customer {} - {}",
                customerId, ldapEx);
            throw new IllegalStateException(ldapEx);
        }
        
        result = null;
        
        try {
            String ouApplicationsDn = new LdapDnBuilder(customerDN).addAttriubte(
                ATTR_OU, OU_APPLICATIONS_NAME).build();
            DeleteRequest request = new DeleteRequest(ouApplicationsDn);
            result = getAppConnPool().delete(request);
        } catch (LDAPException ldapEx) {
            audit.fail(ldapEx.getExceptionMessage());
            getLogger().error("Could not perform delete for customer {} - {}",
                customerId, ldapEx);
            throw new IllegalStateException(ldapEx);
        }
        
        result = null;
        
        try {
            String ouPeopleDn = new LdapDnBuilder(customerDN).addAttriubte(
                ATTR_OU, OU_PEOPLE_NAME).build();
            DeleteRequest request = new DeleteRequest(ouPeopleDn);
            result = getAppConnPool().delete(request);
        } catch (LDAPException ldapEx) {
            audit.fail(ldapEx.getExceptionMessage());
            getLogger().error("Could not perform delete for customer {} - {}",
                customerId, ldapEx);
            throw new IllegalStateException(ldapEx);
        }
        
        result = null;
        
        try {
            DeleteRequest request = new DeleteRequest(customerDN);
            request.addControl(new SubtreeDeleteRequestControl());
            result = getAppConnPool().delete(request);
        } catch (LDAPException ldapEx) {
            audit.fail();
            getLogger().error(
                "Could not perform delete for customerId {} - {}", customerId,
                ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            audit.fail();
            getLogger().error(
                "Error deleting customer groups for customerId {} - {}",
                customerId, result.getResultCode());
            throw new IllegalStateException(
                String
                    .format(
                        "LDAP error encountered when deleting customer groups for customerId: %s - %s",
                        customerId, result.getResultCode().toString()));
        }
        audit.succeed();

        getLogger().info("Deleted customer {}", customerId);
    }

    public List<Customer> findAll() {
        getLogger().debug("Search all customers");
        SearchResult searchResult = null;

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_SOFT_DELETED, String.valueOf(false))
            .addEqualAttribute(ATTR_OBJECT_CLASS,
                OBJECTCLASS_RACKSPACEORGANIZATION).build();

        try {
            searchResult = getAppConnPool().search(BASE_DN, SearchScope.ONE,
                searchFilter);
        } catch (LDAPSearchException ldapEx) {
            getLogger().error(
                "Error searching for all customers under DN {} - {}", BASE_DN,
                ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        List<Customer> customers = new ArrayList<Customer>();
        for (SearchResultEntry e : searchResult.getSearchEntries()) {
            Customer customer = getCustomer(e);
            customers.add(customer);
        }

        getLogger().debug("Found {} clients under DN {}", customers.size(),
            BASE_DN);
        return customers;
    }

    public Customer findByCustomerId(String customerId) {
        getLogger().debug("Doing search for customerId {}", customerId);

        if (StringUtils.isBlank(customerId)) {
            getLogger().error("Null or Empty customerId parameter");
            throw new IllegalArgumentException(
                "Null or Empty customerId parameter.");
        }

        Customer customer = null;
        SearchResult searchResult = getCustomerSearchResult(customerId);

        if (searchResult.getEntryCount() == 1) {
            SearchResultEntry e = searchResult.getSearchEntries().get(0);
            customer = getCustomer(e);
        } else if (searchResult.getEntryCount() > 1) {
            getLogger().error(
                "More than one entry was found for customerId {}", customerId);
            throw new IllegalStateException(
                "More than one entry was found for this customerId");
        }

        getLogger().debug("Found customer - {}", customer);

        return customer;
    }

    public Customer findByInum(String customerInum) {
        getLogger().debug("Doing search for customerInum {}", customerInum);

        if (StringUtils.isBlank(customerInum)) {
            getLogger().error("Null or Empty customerInum parameter");
            throw new IllegalArgumentException(
                "Null or Empty customerInum parameter.");
        }

        Customer customer = null;
        SearchResult searchResult = null;

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_INUM, customerInum)
            .addEqualAttribute(ATTR_OBJECT_CLASS,
                OBJECTCLASS_RACKSPACEORGANIZATION).build();

        try {
            searchResult = getAppConnPool().search(BASE_DN, SearchScope.ONE,
                searchFilter);
        } catch (LDAPSearchException ldapEx) {
            getLogger().error("Error searching for customerInum {} - {}",
                customerInum, ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (searchResult.getEntryCount() == 1) {
            SearchResultEntry e = searchResult.getSearchEntries().get(0);
            customer = getCustomer(e);
        } else if (searchResult.getEntryCount() > 1) {
            getLogger().error(
                "More than one entry was found for customerInum {}",
                customerInum);
            throw new IllegalStateException(
                "More than one entry was found for this customerInum");
        }

        getLogger().debug("Found customer - {}", customer);

        return customer;
    }

    public String getCustomerDnByCustomerId(String customerId) {
        String dn = null;
        SearchResult searchResult = getCustomerSearchResult(customerId);
        if (searchResult.getEntryCount() == 1) {
            SearchResultEntry e = searchResult.getSearchEntries().get(0);
            dn = e.getDN();
        } else if (searchResult.getEntryCount() > 1) {
            getLogger().error(
                "More than one entry was found for customerId {}", customerId);
            throw new IllegalStateException(
                "More than one entry was found for this customerId");
        }
        return dn;
    }

    public String getUnusedCustomerInum() {
        // TODO: We might may this call to the XDI server in the future.
        Customer customer = null;
        String inum = "";
        do {
            inum = this.getRackspaceInumPrefix() + InumHelper.getRandomInum(2);
            customer = findByInum(inum);
        } while (customer != null);

        return inum;
    }

    public void save(Customer customer) {
        getLogger().debug("Updating customer {}", customer);

        if (customer == null || StringUtils.isBlank(customer.getCustomerId())) {
            getLogger().error(
                "Customer instance is null or its customerId has no value");
            throw new IllegalArgumentException(
                "Bad parameter: The Customer instance either null or its customerId has no value.");
        }

        Customer oldCustomer = findByCustomerId(customer.getCustomerId());

        if (oldCustomer == null) {
            getLogger().error("No record found for customer {}",
                customer.getCustomerId());
            throw new IllegalArgumentException(
                "There is no exisiting record for the given customer instance.");
        }

        if (customer.equals(oldCustomer)) {
            // No changes!
            return;
        }

        LDAPResult result = null;
        List<Modification> mods = getModifications(oldCustomer, customer);
        Audit audit = Audit.log(oldCustomer).modify(mods);
        try {
			result = getAppConnPool().modify(
                getCustomerDnByCustomerId(customer.getCustomerId()),
                mods);
        } catch (LDAPException ldapEx) {
        	audit.fail();
            getLogger().error("Error updating customer {} - {}", customer,
                ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            audit.fail();
            getLogger().error("Error updating customer {} - {}",
                customer.getCustomerId(), result.getResultCode());
            throw new IllegalArgumentException(String.format(
                "LDAP error encountered when updating customer: %s - %s",
                customer.getCustomerId(), result.getResultCode().toString()));
        }
        audit.succeed();

        getLogger().debug("Updated customer {}", customer.getCustomerId());
    }

    private Customer getCustomer(SearchResultEntry resultEntry) {
        Customer customer = new Customer();

        customer.setUniqueId(resultEntry.getDN());
        customer.setCustomerId(resultEntry
            .getAttributeValue(ATTR_RACKSPACE_CUSTOMER_NUMBER));
        customer.setInum(resultEntry.getAttributeValue(ATTR_INUM));
        customer.setIname(resultEntry.getAttributeValue(ATTR_INAME));
        
        customer.setPasswordRotationDuration(resultEntry.getAttributeValueAsInteger(ATTR_PASSWORD_ROTATION_DURATION));
        customer.setPasswordRotationEnabled(resultEntry.getAttributeValueAsBoolean(ATTR_PASSWORD_ROTATION_ENABLED));    
        
        String statusStr = resultEntry.getAttributeValue(ATTR_STATUS);
        if (statusStr != null) {
            customer.setStatus(Enum.valueOf(CustomerStatus.class,
                statusStr.toUpperCase()));
        }

        String deleted = resultEntry.getAttributeValue(ATTR_SOFT_DELETED);
        if (deleted != null) {
            customer.setSoftDeleted(resultEntry
                .getAttributeValueAsBoolean(ATTR_SOFT_DELETED));
        }

        String locked = resultEntry.getAttributeValue(ATTR_LOCKED);
        if (locked != null) {
            customer.setLocked(resultEntry
                .getAttributeValueAsBoolean(ATTR_LOCKED));
        }
 
        return customer;
    }

    private SearchResult getCustomerSearchResult(String customerId) {
        SearchResult searchResult = null;

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_RACKSPACE_CUSTOMER_NUMBER, customerId)
            .addEqualAttribute(ATTR_SOFT_DELETED, String.valueOf(false))
            .addEqualAttribute(ATTR_OBJECT_CLASS,
                OBJECTCLASS_RACKSPACEORGANIZATION).build();

        try {
            searchResult = getAppConnPool().search(BASE_DN, SearchScope.ONE,
                searchFilter);
        } catch (LDAPSearchException ldapEx) {
            getLogger().error("Error searching for customerId {} - {}",
                customerId, ldapEx);
            throw new IllegalStateException(ldapEx);
        }
        return searchResult;
    }

    List<Modification> getModifications(Customer cOld, Customer cNew) {
        List<Modification> mods = new ArrayList<Modification>();

        if (cNew.getIname() != null) {
            if (StringUtils.isBlank(cNew.getIname())) {
                mods.add(new Modification(ModificationType.DELETE, ATTR_INAME));
            } else if (!StringUtils.equals(cOld.getIname(), cNew.getIname())) {
                mods.add(new Modification(ModificationType.REPLACE, ATTR_INAME,
                    cNew.getIname()));
            }
        }

        if (cNew.getStatus() != null
            && !cOld.getStatus().equals(cNew.getStatus())) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_STATUS,
                cNew.getStatus().toString()));
        }

        if (cNew.isLocked() != null
            && cNew.isLocked() != cOld.isLocked()) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_LOCKED,
                String.valueOf(cNew.isLocked())));
        }

        if (cNew.getSoftDeleted() != null
            && cNew.getSoftDeleted() != cOld.getSoftDeleted()) {
            mods.add(new Modification(ModificationType.REPLACE,
                ATTR_SOFT_DELETED, String.valueOf(cNew.getSoftDeleted())));
        }
        
        if (cNew.getPasswordRotationEnabled() != null
            && cNew.getPasswordRotationEnabled() != cOld.getPasswordRotationEnabled()) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_PASSWORD_ROTATION_ENABLED, 
                String.valueOf(cNew.getPasswordRotationEnabled())));
        }
        
        if (cNew.getPasswordRotationDuration() != cOld.getPasswordRotationDuration()) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_PASSWORD_ROTATION_DURATION, 
                String.valueOf(cNew.getPasswordRotationDuration())));
        }
        return mods;
    }
}
