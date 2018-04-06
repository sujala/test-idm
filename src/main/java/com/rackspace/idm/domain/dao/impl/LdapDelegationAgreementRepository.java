package com.rackspace.idm.domain.dao.impl;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.PrincipalType;
import com.rackspace.idm.annotation.LDAPComponent;
import com.rackspace.idm.api.resource.cloud.v20.FindDelegationAgreementParams;
import com.rackspace.idm.domain.dao.DelegationAgreementDao;
import com.rackspace.idm.domain.dao.IdentityUserDao;
import com.rackspace.idm.domain.entity.DelegateType;
import com.rackspace.idm.domain.entity.DelegationAgreement;
import com.rackspace.idm.domain.entity.DelegationPrincipal;
import com.rackspace.idm.domain.entity.EndUser;
import com.rackspace.idm.exception.SizeLimitExceededException;
import com.rackspace.idm.modules.endpointassignment.entity.Rule;
import com.rackspace.idm.modules.usergroups.Constants;
import com.rackspace.idm.modules.usergroups.dao.UserGroupDao;
import com.unboundid.ldap.sdk.*;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

@LDAPComponent
public class LdapDelegationAgreementRepository extends LdapGenericRepository<DelegationAgreement> implements DelegationAgreementDao {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final String SIZE_EXCEEDED_ERROR_MESSAGE = "Aborting loading delegation agreements. Result size exceeded max directory limit";
    public static final String SIZE_EXCEEDED_EXCEPTION_MESSAGE = "Result size exceeded.";

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

    @Override
    public void updateAgreement(DelegationAgreement delegationAgreement) {
        updateObject(delegationAgreement);
    }

    /**
     *
     * @param findDelegationAgreementParams
     * @return
     */
    @Override
    public List<DelegationAgreement> findDelegationAgreements(FindDelegationAgreementParams findDelegationAgreementParams) {
        List<DN> delegateDnOptions = new ArrayList<>();
        List<DN> principalDnOptions = new ArrayList<>();

        DN delegateDN = null;
        DN principalDN = null;
        if (findDelegationAgreementParams.getDelegate() != null) {
            delegateDN = findDelegationAgreementParams.getDelegate().getDn();
            delegateDnOptions.add(delegateDN);

            if (findDelegationAgreementParams.getDelegate().getDelegateReference().getDelegateType() == DelegateType.USER) {
                delegateDnOptions.addAll(((EndUser)findDelegationAgreementParams.getDelegate()).getUserGroupDNs());
            }
        }
        if (findDelegationAgreementParams.getPrincipal() != null) {
            principalDN = findDelegationAgreementParams.getPrincipal().getDn();
            principalDnOptions.add(principalDN);

            if (findDelegationAgreementParams.getPrincipal().getPrincipalType() == PrincipalType.USER) {
                principalDnOptions.addAll(((EndUser)findDelegationAgreementParams.getPrincipal()).getUserGroupDNs());
            }
        }

        if (delegateDN == null && principalDN == null) {
            throw new IllegalArgumentException("Must provide a delegate or principal to limit search");
        }

        LdapSearchBuilder builder = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_DELEGATION_AGREEMENT);

        // Add Delegate Filters
        List<Filter> orComponents = new ArrayList<Filter>();
        if (CollectionUtils.isNotEmpty(delegateDnOptions)) {
            for (DN dn : delegateDnOptions) {
                orComponents.add(Filter.createEqualityFilter(ATTR_MEMBER, dn.toString()));
            }
        }

        // Add Principal Filters
        if (CollectionUtils.isNotEmpty(principalDnOptions)) {
            for (DN dn : principalDnOptions) {
                orComponents.add(Filter.createEqualityFilter(ATTR_RS_PRINCIPAL_DN, dn.toString()));
            }
        }

        builder.addOrAttributes(orComponents);

        try {
            // TODO - This arbitrarily cuts off max to 100 since every result requires a second query to pull the principal
            // We've said before that we need to limit number of DAs that can be created and assigned and such.
            List<DelegationAgreement> das = getUnpagedUnsortedObjects(builder.build(), getBaseDn(), SearchScope.SUB, 100);
            return das;
        } catch (LDAPSearchException ldapEx) {
            if (ldapEx.getResultCode() == ResultCode.SIZE_LIMIT_EXCEEDED) {
                logger.warn(SIZE_EXCEEDED_ERROR_MESSAGE, ldapEx);
                throw new SizeLimitExceededException(SIZE_EXCEEDED_EXCEPTION_MESSAGE, ldapEx);
            } else {
                throw new IllegalStateException(ldapEx);
            }
        }
    }

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
