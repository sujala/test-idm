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

    /**
     * Counts all OTP devices giving the parent.
     *
     * @param parent
     * @return
     */
    int countOTPDevicesByParent(UniqueId parent);

    /**
     * Counts all verified OTP devices giving the parent.
     *
     * @param parent
     * @return
     */
    int countVerifiedOTPDevicesByParent(UniqueId parent);

    /**
     * Returns the list of verified OTP devices giving the parent.
     *
     * @param parent
     * @return
     */
    Iterable<OTPDevice> getVerifiedOTPDevicesByParent(UniqueId parent);

    /**
     * Returns the list of OTP devices, regardless of status, associated with the parent.
     *
     * @param parent
     * @return
     */
    Iterable<OTPDevice> getOTPDevicesByParent(UniqueId parent);

    /**
     * Removes all OTP devices from a parent.
     *
     * @param parent
     */
    void deleteAllOTPDevicesFromParent(UniqueId parent);

}
