package com.rackspace.idm.domain.dao.impl;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.PrincipalType;
import com.rackspace.idm.annotation.LDAPComponent;
import com.rackspace.idm.domain.dao.DelegationAgreementDao;
import com.rackspace.idm.domain.dao.IdentityUserDao;
import com.rackspace.idm.domain.entity.DelegationAgreement;
import com.rackspace.idm.domain.entity.DelegationPrincipal;
import com.rackspace.idm.modules.usergroups.Constants;
import com.rackspace.idm.modules.usergroups.dao.LdapUserGroupRepository;
import com.rackspace.idm.modules.usergroups.dao.UserGroupDao;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.SearchScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@LDAPComponent
public class LdapDelegationAgreementRepository extends LdapGenericRepository<DelegationAgreement> implements DelegationAgreementDao {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    IdentityUserDao identityUserDao;

    @Autowired
    UserGroupDao userGroupDao;

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

    @Override
    public void doPostEncode(DelegationAgreement object) {
        // The principal DN must be looked up to account for differences between DNs for fed users and provisioned users
        if (object.getPrincipalDN() != null) {
            DN principalDN = object.getPrincipalDN();
            Object rawObject = null;
            try {
                // Need to use the specific daos to ensure the correct `doPostDecode` is run for the entries returned.
                if (principalDN.isDescendantOf(Constants.USER_GROUP_BASE_DN, false)) {
                    rawObject = userGroupDao.getGroupByDn(principalDN);
                } else if (principalDN.isDescendantOf(LdapRepository.USERS_BASE_DN, false)
                        || principalDN.isDescendantOf(LdapRepository.EXTERNAL_PROVIDERS_BASE_DN, false)) {
                    rawObject = identityUserDao.getEndUserByDn(principalDN);
                }
            } catch (Exception e) {
                logger.error(String.format("Delegation agreement '%s' contains invalid principal. Ignoring principal.", object.getId()), e);
            }

            DelegationPrincipal delegationPrincipal = null;
            if (rawObject != null && rawObject instanceof DelegationPrincipal) {
                delegationPrincipal = (DelegationPrincipal) rawObject;
                // Set with the converted principal
                object.setPrincipal(delegationPrincipal);
            } else {
                object.setPrincipalDN(null);
                logger.error(String.format("Delegation agreement '%s' contains invalid principal. Ignoring principal.", object.getId()));
            }
        }
    }
}
