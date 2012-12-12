package com.rackspace.idm.domain.dao.impl;

import org.springframework.stereotype.Component;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.dao.CustomerDao;
import com.rackspace.idm.domain.entity.Customer;
import com.unboundid.ldap.sdk.*;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
public class LdapCustomerRepository extends LdapRepository implements
    CustomerDao {

    @Override
    public void addCustomer(Customer customer) {
        getLogger().info("Adding customer {}", customer);
        if (customer == null) {
            String errorMsg = "Null instance of Customer was passed.";
            getLogger().error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        Attribute[] attributes = getAddAttributes(customer);

        String customerDN = new LdapDnBuilder(CUSTOMERS_BASE_DN).addAttribute(ATTR_ID,
            customer.getId()).build();

        customer.setUniqueId(customerDN);

        Audit audit = Audit.log(customer).add();

        this.addEntry(customerDN, attributes, audit);

        audit.succeed();
        getLogger().info("Added customer {}", customer);
    }


    @Override
    public void deleteCustomer(String customerId) {
        getLogger().info("Deleting customer {}", customerId);
        if (StringUtils.isBlank(customerId)) {
            String errMsg = "Null or Empty customerId paramter";
            getLogger().error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        Customer customer = getCustomerByCustomerId(customerId);

        String customerDN = customer.getUniqueId();

        Audit audit = Audit.log(customer).delete();

        this.deleteEntryAndSubtree(customerDN, audit);

        audit.succeed();

        getLogger().info("Deleted customer {}", customerId);
    }

    
    @Override
    public List<Customer> getAllCustomers() {
        getLogger().debug("Getting all customers");

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_OBJECT_CLASS,
                OBJECTCLASS_RACKSPACEORGANIZATION).build();

        List<SearchResultEntry> entries = this.getMultipleEntries(CUSTOMERS_BASE_DN,
            SearchScope.ONE, searchFilter, ATTR_RACKSPACE_CUSTOMER_NUMBER);

        List<Customer> customers = new ArrayList<Customer>();
        for (SearchResultEntry e : entries) {
            Customer customer = getCustomer(e);
            customers.add(customer);
        }

        getLogger().debug("Found {} customers under DN {}", customers.size(),
            BASE_DN);
        return customers;
    }

    @Override
    public Customer getCustomerById(String id) {
        getLogger().debug("Doing search for id {}", id);

        if (StringUtils.isBlank(id)) {
            String errMsg = "Null or Empty id paramter";
            getLogger().error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        Customer customer = null;

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_ID, id)
            .addEqualAttribute(ATTR_OBJECT_CLASS,
                OBJECTCLASS_RACKSPACEORGANIZATION).build();

        SearchResultEntry entry = this.getSingleEntry(CUSTOMERS_BASE_DN, SearchScope.ONE,
            searchFilter);

        if (entry != null) {
            customer = getCustomer(entry);
        }

        getLogger().debug("Found customer - {}", customer);

        return customer;
    }
    
    @Override
    public Customer getCustomerByCustomerId(String customerId) {
        getLogger().debug("Doing search for customerId {}", customerId);

        if (StringUtils.isBlank(customerId)) {
            String errMsg = "Null or Empty customerId parameter";
            getLogger().debug(errMsg);
            return null;
        }

        Customer customer = null;

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_RACKSPACE_CUSTOMER_NUMBER, customerId)
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEORGANIZATION).build();

        SearchResultEntry entry = this.getSingleEntry(CUSTOMERS_BASE_DN, SearchScope.ONE, searchFilter);

        if (entry != null) {
            customer = getCustomer(entry);
        }

        getLogger().debug("Found customer - {}", customer);

        return customer;
    }
    
    @Override
    public void updateCustomer(Customer customer) {
        getLogger().info("Updating customer {}", customer);

        if (customer == null || StringUtils.isBlank(customer.getRcn())) {
            String errMsg = "Customer instance is null or its customerId has no value";
            getLogger().error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        Customer oldCustomer = getCustomerByCustomerId(customer.getRcn());

        if (oldCustomer == null) {
            String errMsg = String.format("No record found for customer %s", customer.getRcn());
            getLogger().error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        List<Modification> mods = getModifications(oldCustomer, customer);

        if (mods.size() == 0) {
            // no changes
            return;
        }

        Audit audit = Audit.log(oldCustomer).modify(mods);
        updateEntry(oldCustomer.getUniqueId(), mods, audit);

        audit.succeed();

        getLogger().info("Updated customer {}", customer);
    }

    Attribute[] getAddAttributes(Customer customer) {
        List<Attribute> atts = new ArrayList<Attribute>();

        atts.add(new Attribute(ATTR_OBJECT_CLASS,
            ATTR_CUSTOMER_OBJECT_CLASS_VALUES));
        
        if (!StringUtils.isBlank(customer.getId())) {
            atts.add(new Attribute(ATTR_ID, customer
                .getId()));
        }

        if (!StringUtils.isBlank(customer.getRcn())) {
            atts.add(new Attribute(ATTR_RACKSPACE_CUSTOMER_NUMBER, customer
                .getRcn()));
        }

        if (customer.isEnabled() != null) {
            atts.add(new Attribute(ATTR_ENABLED, String.valueOf(customer
                .isEnabled()).toUpperCase()));
        }

        Attribute[] attributes = atts.toArray(new Attribute[0]);

        return attributes;
    }

    Customer getCustomer(SearchResultEntry resultEntry) {
        Customer customer = new Customer();

        customer.setUniqueId(resultEntry.getDN());
        customer.setRcn(resultEntry
                .getAttributeValue(ATTR_RACKSPACE_CUSTOMER_NUMBER));

        customer.setPasswordRotationDuration(resultEntry
            .getAttributeValueAsInteger(ATTR_PASSWORD_ROTATION_DURATION));
        customer.setPasswordRotationEnabled(resultEntry
            .getAttributeValueAsBoolean(ATTR_PASSWORD_ROTATION_ENABLED));

        customer.setEnabled(resultEntry.getAttributeValueAsBoolean(ATTR_ENABLED));

        return customer;
    }

    List<Modification> getModifications(Customer cOld, Customer cNew) {
        List<Modification> mods = new ArrayList<Modification>();

        if (cNew.isEnabled() != null && cNew.isEnabled() != cOld.isEnabled()) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_ENABLED,
                String.valueOf(cNew.isEnabled()).toUpperCase()));
        }

        if (cNew.getPasswordRotationEnabled() != null
            && cNew.getPasswordRotationEnabled() != cOld
                .getPasswordRotationEnabled()) {
            mods.add(new Modification(ModificationType.REPLACE,
                ATTR_PASSWORD_ROTATION_ENABLED, String.valueOf(cNew
                    .getPasswordRotationEnabled()).toUpperCase()));
        }

        if (cNew.getPasswordRotationDuration() != cOld
            .getPasswordRotationDuration()) {
            mods.add(new Modification(ModificationType.REPLACE,
                ATTR_PASSWORD_ROTATION_DURATION, String.valueOf(cNew
                    .getPasswordRotationDuration())));
        }

        return mods;
    }
    
    @Override
    public String getNextCustomerId() {
        return getNextId(NEXT_CUSTOMER_ID);
    }
    
    @Override
    public void softDeleteCustomer(Customer customer) {
        getLogger().info("SoftDeleting customer - {}", customer.getRcn());
        try {
            String oldDn = customer.getUniqueId();
            String newRdn = new LdapDnBuilder("").addAttribute(ATTR_ID, customer.getId()).build();
            String newDn = new LdapDnBuilder(SOFT_DELETED_CUSTOMERS_BASE_DN).addAttribute(ATTR_ID, customer.getId()).build();
            // Modify the customer
            getAppInterface().modifyDN(oldDn, newRdn, false, SOFT_DELETED_CUSTOMERS_BASE_DN);
            customer.setUniqueId(newDn);
            // Disabled the customer
            getAppInterface().modify(customer.getUniqueId(), new Modification(
                ModificationType.REPLACE, ATTR_ENABLED, String.valueOf(false).toUpperCase()));
        } catch (LDAPException e) {
            getLogger().error("Error soft deleting customer", e);
            throw new IllegalStateException(e.getMessage(), e);
        }
        getLogger().info("SoftDeleted customer - {}", customer.getRcn());
    }
    
    @Override
    public Customer getSoftDeletedCustomerById(String id) {

        getLogger().debug("Doing search for id " + id);
        if (StringUtils.isBlank(id)) {
            getLogger().error("Null or Empty id parameter");
            throw new IllegalArgumentException("Null or Empty id parameter.");
        }

        Customer customer = null;

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_ID, id)
            .addEqualAttribute(ATTR_OBJECT_CLASS,
                OBJECTCLASS_RACKSPACEORGANIZATION).build();

        SearchResultEntry entry = this.getSingleEntry(SOFT_DELETED_CUSTOMERS_BASE_DN, SearchScope.ONE,
            searchFilter);

        if (entry != null) {
            customer = getCustomer(entry);
        }
        
        return customer;
    }

    @Override
    public Customer getSoftDeletedUserByCustomerId(String customerId) {

        getLogger().debug("Doing search for customerId " + customerId);
        if (StringUtils.isBlank(customerId)) {
            getLogger().error("Null or Empty customerId parameter");
            throw new IllegalArgumentException(
                "Null or Empty customerId parameter.");
        }

        Customer customer = null;

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_RACKSPACE_CUSTOMER_NUMBER, customerId)
            .addEqualAttribute(ATTR_OBJECT_CLASS,
                OBJECTCLASS_RACKSPACEORGANIZATION).build();

        SearchResultEntry entry = this.getSingleEntry(SOFT_DELETED_CUSTOMERS_BASE_DN, SearchScope.ONE,
            searchFilter);

        if (entry != null) {
            customer = getCustomer(entry);
        }

        getLogger().debug("Found customer - {}", customer);

        return customer;
    }

    @Override
    public void unSoftDeleteCustomer(Customer customer) {
        getLogger().info("SoftDeleting customer - {}", customer);
        try {
            String oldDn = customer.getUniqueId();
            String newRdn = new LdapDnBuilder("").addAttribute(ATTR_ID,
                customer.getId()).build();
            String newDn = new LdapDnBuilder(CUSTOMERS_BASE_DN)
            .addAttribute(ATTR_ID, customer.getId()).build();
            // Modify the User
            getAppInterface().modifyDN(oldDn, newRdn, false, CUSTOMERS_BASE_DN);
            customer.setUniqueId(newDn);
            // Enabled the User
            getAppInterface().modify(customer.getUniqueId(), new Modification(
                ModificationType.REPLACE, ATTR_ENABLED, String.valueOf(true).toUpperCase()));
        } catch (LDAPException e) {
            getLogger().error("Error soft deleting customer", e);
            throw new IllegalStateException(e.getMessage(), e);
        }
        getLogger().info("SoftDeleted customer - {}", customer);
    }
}
