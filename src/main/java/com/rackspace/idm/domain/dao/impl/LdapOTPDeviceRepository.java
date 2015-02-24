package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.dao.OTPDeviceDao;
import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.entity.OTPDevice;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.SearchResultEntry;
import org.springframework.stereotype.Component;

@Component
public class LdapOTPDeviceRepository extends LdapGenericRepository<OTPDevice> implements OTPDeviceDao {

    @Override
    public String getBaseDn() {
        return USERS_BASE_DN;
    }

    @Override
    public String getLdapEntityClass() {
        return OBJECTCLASS_OTP_DEVICE;
    }

    @Override
    public String getSortAttribute() {
        return ATTR_ID;
    }

    @Override
    public void addOTPDevice(UniqueId parent, OTPDevice otpDevice) {
        addObject(addLdapContainer(parent.getUniqueId(), LdapRepository.CONTAINER_OTP_DEVICES), otpDevice);
    }

    @Override
    public OTPDevice getOTPDeviceByParentAndId(UniqueId parent, String id) {
        final Filter filter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_OTP_DEVICE)
                .addEqualAttribute(ATTR_ID, id).build();
        return getObject(filter, getContainerDN(parent));
    }

    private String getContainerDN(UniqueId parent) {
        return new LdapDnBuilder(parent.getUniqueId())
                .addAttribute(ATTR_NAME, LdapRepository.CONTAINER_OTP_DEVICES).build();
    }

}
