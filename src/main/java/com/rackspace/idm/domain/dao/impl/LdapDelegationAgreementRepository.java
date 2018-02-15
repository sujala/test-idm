package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.LDAPComponent;
import com.rackspace.idm.domain.dao.DelegationAgreementDao;
import com.rackspace.idm.domain.entity.DelegationAgreement;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.SearchScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@LDAPComponent
public class LdapDelegationAgreementRepository extends LdapGenericRepository<DelegationAgreement> implements DelegationAgreementDao {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public String getBaseDn() {
        return DELEGATION_AGREEMENT_BASE_DN;
    }

    @Override
    public String getSortAttribute() {
        return ATTR_NAME;
    }

    @Override
    public String getNextAgreementId() {
        return getUuid();
    }

    @Override
    public void addAgreement(DelegationAgreement delegationAgreement) {
        delegationAgreement.setId(getNextAgreementId());
        addObject(delegationAgreement);
    }

    @Override
    public DelegationAgreement getAgreementById(String agreementId) {
        return getObject(searchByIdFilter(agreementId), SearchScope.SUB);
    }

    @Override
    public void deleteAgreement(DelegationAgreement delegationAgreement) { deleteObject(delegationAgreement); }

    Filter searchByIdFilter(String id) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_ID, id)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_DELEGATION_AGREEMENT).build();
    }
}
