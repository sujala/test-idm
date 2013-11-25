package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.MobilePhone;

/**
 */
public interface MobilePhoneDao extends GenericDao<MobilePhone> {

    /**
     * Searches for a mobile phone with the specified externalMultiFactorPhoneId. At most one entry should be retrieved. Null returned if
     * no entry found.
     *
     * @param externalMultiFactorPhoneId
     * @throws com.unboundid.ldap.sdk.LDAPSearchException if there was an error retrieving the result or more than one result was found
     *
     * @return
     */
    MobilePhone getByExternalId(String externalMultiFactorPhoneId);

    /**
     * Searches for a mobile phone with the specified rsId. At most one entry should be retrieved. Null returned if
     * no entry found.
     *
     * @param id
     * @throws com.unboundid.ldap.sdk.LDAPSearchException if there was an error retrieving the result or more than one result was found
     *
     * @return
     */
    MobilePhone getById(String id);

    /**
     * Retrieves the mobile phone with the given mobile phone number or null if not found.
     *
     * @param telephoneNumber
     * @return
     */
    MobilePhone getByTelephoneNumber(String telephoneNumber);
}
