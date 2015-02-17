package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.OTPDevice;

public interface OTPDeviceDao extends GenericDao<OTPDevice> {

    /**
     * Add an OTP device to a parent.
     *
     * @param parent The parent reference.
     * @param otpDevice The OTP device to be persisted.
     */
    void addOTPDevice(UniqueId parent, OTPDevice otpDevice);

    /**
     * Retrieves the OTP device giving the ID and the parent.
     *
     * @param parent The parent reference.
     * @param id The OTP device ID.
     * @return The OTP device, null if not found.
     */
    OTPDevice getOTPDeviceByParentAndId(UniqueId parent, String id);

}
