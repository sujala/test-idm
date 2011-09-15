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
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;

public class LdapCustomerRepository extends LdapRepository implements
    CustomerDao {

    public LdapCustomerRepository(LdapConnectionPools connPools,
        Configuration config) {
        super(connPools, config);
    }

    
    @Override
    public void addCustomer(Customer customer) {
        getLogger().info("Adding customer {}", customer);
        if (customer == null) {
            String errorMsg = "Null instance of Customer was passed.";
            getLogger().error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        Attribute[] attributes = getAddAttributes(customer);

        String customerDN = new LdapDnBuilder(CUSTOMERS_BASE_DN).addAttribute(ATTR_O,
            customer.getInum()).build();

        customer.setUniqueId(customerDN);

        Audit audit = Audit.log(customer).add();
        LDAPConnection conn = getAppPoolConnection(audit);
        this.addEntry(conn, customerDN, attributes, audit);

        audit.succeed();

        getAppConnPool().releaseConnection(conn);

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
            .addEqualAttribute(ATTR_SOFT_DELETED, String.valueOf(false))
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
    public Customer getCustomerByCustomerId(String customerId) {
        getLogger().debug("Doing search for customerId {}", customerId);

        if (StringUtils.isBlank(customerId)) {
            String errMsg = "Null or Empty customerId paramter";
            getLogger().error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        Customer customer = null;

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_RACKSPACE_CUSTOMER_NUMBER, customerId)
            .addEqualAttribute(ATTR_SOFT_DELETED, String.valueOf(false))
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
    public Customer getCustomerByInum(String customerInum) {
        getLogger().debug("Doing search for customerInum {}", customerInum);

        if (StringUtils.isBlank(customerInum)) {
            String errMsg = "Null or Empty customerInum paramter";
            getLogger().error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        Customer customer = null;

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_INUM, customerInum)
            .addEqualAttribute(ATTR_OBJECT_CLASS,
                OBJECTCLASS_RACKSPACEORGANIZATION).build();

        SearchResultEntry entry = this.getSingleEntry(CUSTOMERS_BASE_DN, SearchScope.ONE,
            searchFilter, ATTR_RACKSPACE_CUSTOMER_NUMBER);

        if (entry != null) {
            customer = getCustomer(entry);
        }

        getLogger().debug("Found customer - {}", customer);

        return customer;
    }

    
    @Override
    public String getUnusedCustomerInum() {
        getLogger().debug("Getting Unused Customer Inum");
        Customer customer = null;
        String inum = "";
        do {
            inum = this.getRackspaceInumPrefix() + InumHelper.getRandomInum(2);
            customer = getCustomerByInum(inum);
        } while (customer != null);
        getLogger().debug("Got Unused Customer Inum {}", inum);
        return inum;
    }

    
    @Override
    public void updateCustomer(Customer customer) {
        getLogger().info("Updating customer {}", customer);

        if (customer == null || StringUtils.isBlank(customer.getCustomerId())) {
            String errMsg = "Customer instance is null or its customerId has no value";
            getLogger().error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        Customer oldCustomer = getCustomerByCustomerId(customer.getCustomerId());

        if (oldCustomer == null) {
            String errMsg = String.format("No record found for customer %s", customer.getCustomerId());
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

    private Attribute[] getAddAttributes(Customer customer) {
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

        return attributes;
    }

    private Customer getCustomer(SearchResultEntry resultEntry) {
        Customer customer = new Customer();

        customer.setUniqueId(resultEntry.getDN());
        customer.setCustomerId(resultEntry
            .getAttributeValue(ATTR_RACKSPACE_CUSTOMER_NUMBER));
        customer.setInum(resultEntry.getAttributeValue(ATTR_INUM));
        customer.setIname(resultEntry.getAttributeValue(ATTR_INAME));

        customer.setPasswordRotationDuration(resultEntry
            .getAttributeValueAsInteger(ATTR_PASSWORD_ROTATION_DURATION));
        customer.setPasswordRotationEnabled(resultEntry
            .getAttributeValueAsBoolean(ATTR_PASSWORD_ROTATION_ENABLED));

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
            && !cNew.getStatus().equals(cOld.getStatus())) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_STATUS,
                cNew.getStatus().toString()));
        }

        if (cNew.isLocked() != null && cNew.isLocked() != cOld.isLocked()) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_LOCKED,
                String.valueOf(cNew.isLocked())));
        }

        if (cNew.getSoftDeleted() != null
            && cNew.getSoftDeleted() != cOld.getSoftDeleted()) {
            mods.add(new Modification(ModificationType.REPLACE,
                ATTR_SOFT_DELETED, String.valueOf(cNew.getSoftDeleted())));
        }

        if (cNew.getPasswordRotationEnabled() != null
            && cNew.getPasswordRotationEnabled() != cOld
                .getPasswordRotationEnabled()) {
            mods.add(new Modification(ModificationType.REPLACE,
                ATTR_PASSWORD_ROTATION_ENABLED, String.valueOf(cNew
                    .getPasswordRotationEnabled())));
        }

        if (cNew.getPasswordRotationDuration() != cOld
            .getPasswordRotationDuration()) {
            mods.add(new Modification(ModificationType.REPLACE,
                ATTR_PASSWORD_ROTATION_DURATION, String.valueOf(cNew
                    .getPasswordRotationDuration())));
        }

        return mods;
    }
}
