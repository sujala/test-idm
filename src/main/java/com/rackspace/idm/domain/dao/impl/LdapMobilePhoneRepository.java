package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.dao.MobilePhoneDao;
import com.rackspace.idm.domain.entity.MobilePhone;
import com.unboundid.ldap.sdk.Filter;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Responsible for storing and retrieving MobilePhones to/from the LDAP repository
 */
@Component
public class LdapMobilePhoneRepository extends LdapGenericRepository<MobilePhone> implements MobilePhoneDao {

    @Override
    public MobilePhone getByExternalId(String externalMultiFactorPhoneId) {
        if (StringUtils.isBlank(externalMultiFactorPhoneId)) {
            return null;
        }
        return getObject(searchByExternalMultiFactorPhoneIdFilter(externalMultiFactorPhoneId));
    }

    @Override
    public MobilePhone getById(String id) {
        return getObject(searchByIdFilter(id));
    }

    @Override
    public MobilePhone getByTelephoneNumber(String telephoneNumber) {
        if (StringUtils.isBlank(telephoneNumber)) {
            return null;
        }
        return getObject(searchByTelephoneNumberFilter(telephoneNumber));
    }

    @Override
    public String getBaseDn(){
        return MULTIFACTOR_MOBILE_PHONE_BASE_DN;
    }

    @Override
    public String getLdapEntityClass(){
        return OBJECTCLASS_MULTIFACTOR_MOBILE_PHONE;
    }

    @Override
    public String getSortAttribute() {
        return ATTR_ID;
    }

    @Override
    public String getNextId() {
        return super.getUuid();
    }

    private Filter searchByExternalMultiFactorPhoneIdFilter(String externalPhoneId) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_EXTERNAL_MULTIFACTOR_PHONE_ID, externalPhoneId)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_MULTIFACTOR_MOBILE_PHONE).build();
    }

    private Filter searchByIdFilter(String rsId) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_ID, rsId)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_MULTIFACTOR_MOBILE_PHONE).build();
    }

    private Filter searchByTelephoneNumberFilter(String telephoneNumber) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_TELEPHONE_NUMBER, telephoneNumber)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_MULTIFACTOR_MOBILE_PHONE).build();
    }
}
