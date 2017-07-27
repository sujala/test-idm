package com.rackspace.idm.domain.dao.impl

import com.rackspace.idm.domain.dao.UniqueId
import com.unboundid.ldap.sdk.DN
import com.unboundid.ldap.sdk.DeleteRequest
import com.unboundid.ldap.sdk.LDAPInterface
import com.unboundid.ldap.sdk.controls.SubtreeDeleteRequestControl
import com.unboundid.ldap.sdk.persist.LDAPPersister

/**
 */
class LdapIntegrationTestSupport<T extends UniqueId> {
    private LDAPInterface con
    private Class<T> clazz;
    LDAPPersister<T> applicationPersister

    LdapIntegrationTestSupport(LDAPInterface con, Class<T> clazz) {
        this.con = con
        this.clazz = clazz;
        applicationPersister = LDAPPersister.getInstance(clazz)
    }

    boolean entryExists(T dn) {
        return con.getEntry(dn.getUniqueId()) != null
    }

    /**
     * This function uses the unboundid sdk directly to add data to ldap and bypass rackspace code
     * @param dn
     * @param obj
     * @return
     */
    def addDirect(T obj, String dn) {
        applicationPersister.add(obj, con, dn);
    }

/**
     * This function uses the unboundid sdk directly to delete data from ldap and bypass all rackspace code
     *
     * @param app - app to be deleted
     * @return
     */
    def deleteDirect(UniqueId obj) {
        if (obj != null) {
            deleteDirect(obj.getUniqueId())
        }
    }

    /**
     * This function uses the unboundid sdk directly to delete data from ldap and bypass all rackspace code
     *
     * @param dn - dn of object to be deleted
     * @return
     */
    def deleteDirect(String dn) {

        DeleteRequest deleteRequest = new DeleteRequest(new DN(dn));
        deleteRequest.addControl(new SubtreeDeleteRequestControl(true));
        con.delete(deleteRequest);
    }
}
