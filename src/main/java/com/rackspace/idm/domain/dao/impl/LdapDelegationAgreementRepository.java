package com.rackspace.idm.domain.dao.impl;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.PrincipalType;
import com.rackspace.idm.annotation.LDAPComponent;
import com.rackspace.idm.api.resource.cloud.v20.FindDelegationAgreementParams;
import com.rackspace.idm.domain.dao.DelegationAgreementDao;
import com.rackspace.idm.domain.dao.IdentityUserDao;
import com.rackspace.idm.domain.entity.DelegateType;
import com.rackspace.idm.domain.entity.DelegationAgreement;
import com.rackspace.idm.domain.entity.DelegationDelegate;
import com.rackspace.idm.domain.entity.DelegationPrincipal;
import com.rackspace.idm.domain.entity.EndUser;
import com.rackspace.idm.exception.SizeLimitExceededException;
import com.rackspace.idm.modules.usergroups.Constants;
import com.rackspace.idm.modules.usergroups.dao.UserGroupDao;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPSearchException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchScope;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collections;
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

        /*
         Update with default when creating new DAs. While the DelegationAgreement entity has preencode/postencode
         defined to default this value to false, callers of this service expect the passed in delegationAgreement to
         reflect any defaults applied when saving the DA. Therefore we need to set the defaults on the object itself.
         */
        if (delegationAgreement.getAllowSubAgreements() == null) {
            delegationAgreement.setAllowSubAgreements(Boolean.FALSE);
        }

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

    @Override
    public List<DelegationDelegate> getDelegationAgreementDelegates(DelegationAgreement delegationAgreement) {
        if (CollectionUtils.isEmpty(delegationAgreement.getDelegates())) {
            return Collections.emptyList();
        }

        List<DelegationDelegate> delegationDelegates = new ArrayList<>(delegationAgreement.getDelegates().size());
        for (DN delegateDn : delegationAgreement.getDelegates()) {
            try {
                DelegationDelegate delegate = getDelegateByDn(delegateDn);
                if (delegate == null) {
                    String errorMsg = String.format("The delegation agreement %s references a missing delegate '%s'", delegationAgreement.getId(), delegateDn.toString());
                    logger.error(errorMsg);
                } else {
                    delegationDelegates.add(delegate);
                }
            } catch (IllegalArgumentException e) {
                String errorMsg = String.format("The delegation agreement %s contains an invalid delegate", delegationAgreement.getId());
                logger.error(errorMsg, e);
                throw new IllegalStateException(errorMsg, e);
            }
        }
        return delegationDelegates;
    }

    @Override
    public DelegationDelegate getDelegateByDn(DN delegateDn) {
        Object rawObject = null;

        // Need to use the specific daos to ensure the correct `doPostDecode` is run for the entries returned.
        try {
            if (delegateDn.isDescendantOf(Constants.USER_GROUP_BASE_DN, false)) {
                rawObject = userGroupDao.getGroupByDn(delegateDn);
            } else if (delegateDn.isDescendantOf(LdapRepository.USERS_BASE_DN, false)
                    || delegateDn.isDescendantOf(LdapRepository.EXTERNAL_PROVIDERS_BASE_DN, false)) {
                rawObject = identityUserDao.getEndUserByDn(delegateDn);
            }
        } catch (LDAPException e) {
            throw new IllegalArgumentException(String.format("The dn '%s' does not represent a permissible delegate", delegateDn.toString()));
        }

        // All references must be of specified type
        if (rawObject != null && !(rawObject instanceof DelegationDelegate)) {
            throw new IllegalArgumentException(String.format("The dn '%s' does not represent a permissible delegate", delegateDn.toString()));
        }

        return (DelegationDelegate) rawObject;
    }

    @Override
    public int countNumberOfDelegationAgreementsByPrincipal(DelegationPrincipal delegationPrincipal) {
        return countObjects(searchDelegationAgreementsByPrincipal(delegationPrincipal));
    }

    Filter searchByIdFilter(String id) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_ID, id)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_DELEGATION_AGREEMENT).build();
    }

    private Filter searchDelegationAgreementsByPrincipal(DelegationPrincipal delegationPrincipal) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_RS_PRINCIPAL_DN, delegationPrincipal.getDn().toString())
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
