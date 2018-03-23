package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.api.resource.cloud.v20.DelegateReference;
import com.rackspace.idm.domain.dao.DelegationAgreementDao;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.rackspace.idm.domain.entity.DelegateType;
import com.rackspace.idm.domain.entity.DelegationAgreement;
import com.rackspace.idm.domain.entity.DelegationDelegate;
import com.rackspace.idm.domain.entity.EndUser;
import com.rackspace.idm.domain.service.DelegationService;
import com.rackspace.idm.domain.service.IdentityUserService;
import com.rackspace.idm.modules.usergroups.Constants;
import com.rackspace.idm.modules.usergroups.service.UserGroupService;
import com.unboundid.ldap.sdk.DN;
import org.apache.commons.lang.Validate;
import org.eclipse.persistence.jpa.jpql.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DefaultDelegationService implements DelegationService {
    private static final Logger logger = LoggerFactory.getLogger(DefaultDelegationService.class);

    @Autowired
    private DelegationAgreementDao delegationAgreementDao;

    @Autowired
    private UserGroupService userGroupService;

    @Autowired
    private IdentityUserService identityUserService;

    @Override
    public DelegationAgreement addDelegationAgreement(DelegationAgreement delegationAgreement) {
        Assert.isNotNull(delegationAgreement, "Delegation agreement must be provided");
        delegationAgreementDao.addAgreement(delegationAgreement);

        return delegationAgreement;
    }

    @Override
    public DelegationAgreement getDelegationAgreementById(String delegationAgreementId) {
        return delegationAgreementDao.getAgreementById(delegationAgreementId);
    }

    @Override
    public void deleteDelegationAgreement(DelegationAgreement delegationAgreement) {
        delegationAgreementDao.deleteAgreement(delegationAgreement);
    }

    @Override
    public void addDelegate(DelegationAgreement delegationAgreement, DelegationDelegate delegate) {
        Validate.notNull(delegationAgreement);
        Validate.notNull(delegate);
        Validate.notNull(delegate.getDn());

        Set<DN> existingDelegates = delegationAgreement.getDelegates();

        if (!existingDelegates.contains(delegate.getDn())) {
            existingDelegates.add(delegate.getDn());
            delegationAgreementDao.updateAgreement(delegationAgreement);
        }
    }

    @Override
    public boolean deleteDelegate(DelegationAgreement delegationAgreement, DelegateReference delegateReference) {
        DN delegateDnToDelete = null;
        Set<DN> existingDelegates = delegationAgreement.getDelegates();

        /*
         Groups, provisioned users and federated users have a globally unique identifier (id). User groups and provisioned
         users include this identifier as the final (leftmost) value for their DN composition and the DN can therefore be uniquely
         determined just by the id. However,
         federated users uses username rather than this id - which only needs to be unique within an IDP. This means
         federated users can't be removed by just knowing the unique identifier of the fed user; we must know the
         username and IDP. Federated users must therefore be looked up to determine the DN to remove.
          */
        DelegationDelegate delegate = getDelegateByReference(delegateReference);
        if (delegate != null) {
            // Globally support removal if the specified delegate exists.
            DN delegateDn = delegate.getDn();
            if (existingDelegates.contains(delegateDn)) {
                delegateDnToDelete = delegateDn;
            }
        } else {
            /*
             Support removal of delegates that no longer exist for user groups and users. If any exception encountered,
             just log failure.
              */
            try {
                for (DN existingDelegate : existingDelegates) {
                    // The "id" is the leftmost value
                    String id = existingDelegate.getRDNString().split("=")[1];
                    if (delegateReference.getId().equalsIgnoreCase(id)) {
                        // The id matches, but need to make sure referencing a user group or provision user DN
                        if (existingDelegate.isAncestorOf(LdapRepository.USERS_BASE_DN, false)
                                || existingDelegate.isAncestorOf(Constants.USER_GROUP_BASE_DN, false)) {

                            delegateDnToDelete = existingDelegate;
                        }
                    }
                }
            } catch (Exception e) {
                logger.info(String.format("Error encountered trying to remove missing delegate '%s' from da '%s'", delegateReference.getId(), delegationAgreement.getId()), e);
            }
        }

        boolean modified = false;
        if (delegateDnToDelete != null) {
            boolean removed = existingDelegates.remove(delegateDnToDelete);
            if (removed) {
                delegationAgreementDao.updateAgreement(delegationAgreement);
                modified = true;
            }
        }
        return modified;
    }

    public DelegationDelegate getDelegateByReference(DelegateReference delegateReference) {
        DelegationDelegate delegate = null;
        if (delegateReference.getDelegateType() == DelegateType.USER) {
            EndUser endUser = identityUserService.getEndUserById(delegateReference.getId());
            if (endUser != null) {
                if (!(endUser instanceof DelegationDelegate)) {
                    /*
                     This should never happen as identity user service will only return end users that are also
                     validate delegates, but include this here all the same as class architecture would allow (EndUser
                     that's not also a DelegationDelegate). In this case, the delegate should be considered non-existant.
                      */
                    logger.error(String.format("Looked up enduser %s, but end user is not allowed to be a delegate", endUser.getId()));
                } else {
                    delegate = (DelegationDelegate) endUser;
                }
            }
        } else if (delegateReference.getDelegateType() == DelegateType.USER_GROUP) {
            delegate = userGroupService.getGroupById(delegateReference.getId());
        }
        return delegate;
    }
}
