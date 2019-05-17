package com.rackspace.idm.domain.entity;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePinStateEnum;
import com.rackspace.idm.api.security.ImmutableTenantRole;
import com.rackspace.idm.domain.service.DomainSubUserDefaults;
import com.unboundid.ldap.sdk.DN;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ProvisionedUserDelegate implements EndUserDelegate {

    private EndUser originalEndUser;
    private DelegationAgreement delegationAgreement;
    private DomainSubUserDefaults subUserDefaults;

    public ProvisionedUserDelegate(DomainSubUserDefaults subUserDefaults, DelegationAgreement delegationAgreement, EndUser originalEndUser) {
        this.originalEndUser = originalEndUser;
        this.delegationAgreement = delegationAgreement;
        this.subUserDefaults = subUserDefaults;
    }

    @Override
    public DelegationAgreement getDelegationAgreement() {
        return delegationAgreement;
    }

    @Override
    public String getRegion() {
        return subUserDefaults.getRegion();
    }

    @Override
    public String getDomainId() {
        return subUserDefaults.getDomainId();
    }

    @Override
    public String getPhonePin() {
        return originalEndUser.getPhonePin();
    }

    @Override
    public void setPhonePin(String pin) {
        originalEndUser.setPhonePin(pin);
    }

    @Override
    public Set<String> getRsGroupId() {
        return subUserDefaults.getRateLimitingGroupIds();
    }

    @Override
    public String getContactId() {
        return originalEndUser.getContactId();
    }

    public Set<ImmutableTenantRole> getDefaultDomainRoles() {
        return subUserDefaults.getSubUserTenantRoles();
    }

    public EndUser getOriginalEndUser() {
        return originalEndUser;
    }

    /**
     * Never pull roles off user.
     *
     * @return
     */
    @Override
    public List<TenantRole> getRoles() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public String getEmail() {
        return originalEndUser.getUsername();
    }

    @Override
    public String getUsername() {
        return originalEndUser.getUsername();
    }

    @Override
    public String getId() {
        return originalEndUser.getId();
    }

    @Override
    public boolean isDisabled() {
        return originalEndUser.isDisabled();
    }

    @Override
    public Set<String> getUserGroupIds() {
        return Collections.emptySet();
    }

    @Override
    public Set<DN> getUserGroupDNs() {
        return Collections.emptySet();
    }

    @Override
    public void setEmail(String email) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public void setRegion(String region) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public void setDomainId(String domainId) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public String getUniqueId() {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public void setUniqueId(String uniqueId) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public String getAuditContext() {
        return String.format("%s;delegationAgreementId=%s", originalEndUser.getAuditContext(), delegationAgreement.getId());
    }

    @Override
    public PhonePinStateEnum getPhonePinState() {
        return originalEndUser.getPhonePinState();
    }

    @Override
    public void recordFailedPinAuthentication() {
        originalEndUser.recordFailedPinAuthentication();
    }

    @Override
    public void recordSuccessfulPinAuthentication() {
        originalEndUser.recordSuccessfulPinAuthentication();
    }
}
