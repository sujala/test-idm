package com.rackspace.idm.dao;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.entities.Customer;
import com.rackspace.idm.entities.CustomerStatus;
import com.rackspace.idm.util.InumHelper;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.DeleteRequest;
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

    private static final String CUSTOMER_FIND_ALL_STRING_NOT_DELETED = "(&(objectClass=rackspaceOrganization)(softDeleted=FALSE))";
    private static final String CUSTOMER_FIND_BY_CUSTOMER_ID_STRING_NOT_DELETED = "(&(objectClass=rackspaceOrganization)(rackspaceCustomerNumber=%s)(softDeleted=FALSE))";
    private static final String CUSTOMER_FIND_BY_INUM_STRING = "(&(objectClass=rackspaceOrganization)(inum=%s))";

    public LdapCustomerRepository(LdapConnectionPools connPools, Configuration config, Logger logger) {
        super(connPools, config, logger);
    }

    public void add(Customer customer) {
        getLogger().info("Adding customer {}", customer);
        if (customer == null) {
            getLogger().error("Null instance of Customer was passed.");
            throw new IllegalArgumentException(
                "Null instance of Customer was passed.");
        }

        List<Attribute> atts = new ArrayList<Attribute>();

        atts.add(new Attribute(ATTR_OBJECT_CLASS, ATTR_CUSTOMER_OBJECT_CLASS_VALUES));

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

        if (!StringUtils.isBlank(customer.getSeeAlso())) {
            atts.add(new Attribute(ATTR_SEE_ALSO, customer.getSeeAlso()));
        }

        if (customer.getIsLocked() != null) {
            atts.add(new Attribute(ATTR_LOCKED, String.valueOf(customer
                .getIsLocked())));
        }

        if (customer.getSoftDeleted() != null) {
            atts.add(new Attribute(ATTR_SOFT_DELETED, String.valueOf(customer
                .getSoftDeleted())));
        }

        Attribute[] attributes = atts.toArray(new Attribute[0]);

        LDAPResult result;

        String customerDN = "o=" + customer.getInum() + "," + BASE_DN;
        customer.setUniqueId(customerDN);

        String customerGroupsDN = "ou=groups," + customerDN;
        String customerPeopleDN = "ou=people," + customerDN;
        String customerApplicationsDN = "ou=applications," + customerDN;

        try {
            result = getAppConnPool().add(customerDN, attributes);
        } catch (LDAPException ldapEx) {
            getLogger()
                .error("Error adding customer {} - {}", customer, ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            getLogger().error("Error adding customerId {} - {}",
                customer.getCustomerId(), result.getResultCode());
            throw new IllegalStateException(String.format(
                "LDAP error encountered when adding customerId: %s - %s",
                customer.getCustomerId(), result.getResultCode().toString()));
        }

        // Add ou=groups under new customer entry
        Attribute[] groupAttributes = {
            new Attribute(ATTR_OBJECT_CLASS, ATTR_OBJECT_CLASS_OU_VALUES),
            new Attribute(ATTR_OU, "groups")};

        try {
            result = getAppConnPool().add(customerGroupsDN, groupAttributes);
        } catch (LDAPException ldapEx) {
            getLogger().error("Error adding customer groups {} - {}", customer,
                ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
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
            new Attribute(ATTR_OU, "people")};

        try {
            result = getAppConnPool().add(customerPeopleDN, peopleAttributes);
        } catch (LDAPException ldapEx) {
            getLogger().error("Error adding customer people {} - {}", customer,
                ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
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
            result = getAppConnPool().add(customerApplicationsDN,
                applicationAttributes);
        } catch (LDAPException ldapEx) {
            getLogger().error("Error adding customer applications {} - {}",
                customer, ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
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

        getLogger().debug("Added customer {}", customer);
    }

    public void delete(String customerId) {
        getLogger().info("Deleting customer {}", customerId);
        if (StringUtils.isBlank(customerId)) {
            getLogger().error("Null or Empty customerId paramter");
            throw new IllegalArgumentException(
                "Null or Empty customerId parameter.");
        }

        String customerDN = getCustomerDnByCustomerId(customerId);

        LDAPResult result = null;
        try {
            DeleteRequest request = new DeleteRequest(customerDN);
            request.addControl(new SubtreeDeleteRequestControl());
            result = getAppConnPool().delete(request);
        } catch (LDAPException ldapEx) {
            getLogger().error(
                "Could not perform delete for customerId {} - {}", customerId,
                ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            getLogger().error(
                "Error deleting customer groups for customerId {} - {}",
                customerId, result.getResultCode());
            throw new IllegalStateException(
                String
                    .format(
                        "LDAP error encountered when deleting customer groups for customerId: %s - %s",
                        customerId, result.getResultCode().toString()));
        }

        getLogger().info("Deleted customer {}", customerId);
    }

    public List<Customer> findAll() {
        getLogger().debug("Search all customers");
        SearchResult searchResult = null;
        try {
            searchResult = getAppConnPool().search(BASE_DN, SearchScope.ONE,
                CUSTOMER_FIND_ALL_STRING_NOT_DELETED);
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
        try {
            searchResult = getAppConnPool().search(BASE_DN, SearchScope.ONE,
                String.format(CUSTOMER_FIND_BY_INUM_STRING, customerInum));
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
        try {
            result = getAppConnPool().modify(
                getCustomerDnByCustomerId(customer.getCustomerId()),
                getModifications(oldCustomer, customer));
        } catch (LDAPException ldapEx) {
            getLogger().error("Error updating customer {} - {}", customer,
                ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            getLogger().error("Error updating customer {} - {}",
                customer.getCustomerId(), result.getResultCode());
            throw new IllegalArgumentException(String.format(
                "LDAP error encountered when updating customer: %s - %s",
                customer.getCustomerId(), result.getResultCode().toString()));
        }

        getLogger().debug("Updated customer {}", customer.getCustomerId());
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

        if (cNew.getSeeAlso() != null) {
            if (StringUtils.isBlank(cNew.getSeeAlso())) {
                mods.add(new Modification(ModificationType.DELETE,
                    ATTR_SEE_ALSO));
            } else if (!StringUtils
                .equals(cOld.getSeeAlso(), cNew.getSeeAlso())) {
                mods.add(new Modification(ModificationType.REPLACE,
                    ATTR_SEE_ALSO, cNew.getSeeAlso()));
            }
        }

        if (cNew.getOwner() != null) {
            if (StringUtils.isBlank(cNew.getOwner())) {
                mods.add(new Modification(ModificationType.DELETE, ATTR_OWNER));
            } else if (!StringUtils.equals(cOld.getOwner(), cNew.getOwner())) {
                mods.add(new Modification(ModificationType.REPLACE, ATTR_OWNER,
                    cNew.getOwner()));
            }
        }

        if (cNew.getIsLocked() != null
            && cNew.getIsLocked() != cOld.getIsLocked()) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_LOCKED,
                String.valueOf(cNew.getIsLocked())));
        }

        if (cNew.getSoftDeleted() != null
            && cNew.getSoftDeleted() != cOld.getSoftDeleted()) {
            mods.add(new Modification(ModificationType.REPLACE,
                ATTR_SOFT_DELETED, String.valueOf(cNew
                    .getSoftDeleted())));
        }

        return mods;
    }

    private Customer getCustomer(SearchResultEntry resultEntry) {
        Customer customer = new Customer();

        customer.setUniqueId(resultEntry.getDN());
        customer.setCustomerId(resultEntry
            .getAttributeValue(ATTR_RACKSPACE_CUSTOMER_NUMBER));
        customer.setInum(resultEntry.getAttributeValue(ATTR_INUM));
        customer.setIname(resultEntry.getAttributeValue(ATTR_INAME));
        String statusStr = resultEntry.getAttributeValue(ATTR_STATUS);
        if (statusStr != null) {
            customer.setStatus(Enum.valueOf(CustomerStatus.class,
                statusStr.toUpperCase()));
        }
        customer.setSeeAlso(resultEntry.getAttributeValue(ATTR_SEE_ALSO));
        customer.setOwner(resultEntry.getAttributeValue(ATTR_OWNER));

        String deleted = resultEntry.getAttributeValue(ATTR_SOFT_DELETED);
        if (deleted != null) {
            customer.setSoftDeleted(resultEntry
                .getAttributeValueAsBoolean(ATTR_SOFT_DELETED));
        }

        String locked = resultEntry.getAttributeValue(ATTR_LOCKED);
        if (locked != null) {
            customer.setIsLocked(resultEntry
                .getAttributeValueAsBoolean(ATTR_LOCKED));
        }

        return customer;
    }

    private SearchResult getCustomerSearchResult(String customerId) {
        SearchResult searchResult = null;
        try {
            searchResult = getAppConnPool().search(
                BASE_DN,
                SearchScope.ONE,
                String.format(CUSTOMER_FIND_BY_CUSTOMER_ID_STRING_NOT_DELETED,
                    customerId));
        } catch (LDAPSearchException ldapEx) {
            getLogger().error("Error searching for customerId {} - {}",
                customerId, ldapEx);
            throw new IllegalStateException(ldapEx);
        }
        return searchResult;
    }

    public String getUnusedCustomerInum() {
        // TODO: We might may this call to the XDI server in the future.
        Customer customer = null;
        String inum = "";
        do {
            inum = this.getRackspaceInumPrefix()
                + InumHelper.getRandomInum(2);
            customer = findByInum(inum);
        } while (customer != null);

        return inum;
    }
}
