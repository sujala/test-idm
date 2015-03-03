package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.dao.OTPDeviceDao;
import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.entity.OTPDevice;
import com.unboundid.ldap.sdk.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class LdapOTPDeviceRepository extends LdapGenericRepository<OTPDevice> implements OTPDeviceDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(LdapOTPDeviceRepository.class);

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

    @Override
    public int countOTPDevicesByParent(UniqueId parent) {
        try {
            final Filter filter = new LdapSearchBuilder()
                    .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_OTP_DEVICE).build();
            return countObjects(filter, getContainerDN(parent));
        } catch (IllegalStateException e) {
            return 0;
        }
    }

    @Override
    public int countVerifiedOTPDevicesByParent(UniqueId parent) {
        try {
            return countObjects(searchVerifiedOTPDevicesByParent(parent), getContainerDN(parent));
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public Iterable<OTPDevice> getVerifiedOTPDevicesByParent(UniqueId parent) {
        try {
            return getObjects(searchVerifiedOTPDevicesByParent(parent), getContainerDN(parent));
        } catch (Exception e) {
            return Collections.EMPTY_LIST;
        }
    }

    private Filter searchVerifiedOTPDevicesByParent(UniqueId parent) {
        final Filter filter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_OTP_DEVICE)
                .addEqualAttribute(ATTR_MULTIFACTOR_DEVICE_VERIFIED, "TRUE").build();
        return filter;
    }

    @Override
    public void deleteAllOTPDevicesFromParent(UniqueId parent) {
        try {
            final String container = getContainerDN(parent);
            deleteEntryAndSubtree(container, Audit.deleteOTP(container));
        } catch (IllegalStateException e) {
            LOGGER.warn("Unable to remove OTP devices", e);
        }
    }

    private String getContainerDN(UniqueId parent) {
        return new LdapDnBuilder(parent.getUniqueId())
                .addAttribute(ATTR_NAME, LdapRepository.CONTAINER_OTP_DEVICES).build();
    }

}
