package com.rackspace.idm.multifactor.service;

import com.google.i18n.phonenumbers.Phonenumber;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.FactorTypeEnum;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.MultiFactor;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.MultiFactorDomain;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.MultiFactorStateEnum;
import com.rackspace.identity.multifactor.domain.MfaAuthenticationResponse;
import com.rackspace.identity.multifactor.domain.Pin;
import com.rackspace.idm.domain.entity.*;

import java.math.BigInteger;
import java.util.List;

/**
 * This is the main interface for interacting with the multi-factor service and abstracts the interaction with explicit providers of
 * multi-factor (e.g. - duo security) as well as local IDM persistent storage.
 */
public interface MultiFactorService {
    /**
     * Associates a user to a mobile phone with the given number. If the same phone number if provided, the result
     * must return the same phone id.
     * <p>
     * If the phone number already exists, the existing phone is linked to the user.
     * <p/>
     * <p>
     * If the phone does not yet exist, a new mobile phone is created and returned. The user is updated to link
     * to the new phone.
     * </p>
     *
     * @param userId
     * @param phoneNumber
     * @throws IllegalArgumentException if mobilePhone contains an id, null or invalid telephoneNumber
     * @throws IllegalStateException    if user is already associated with a mobile phone
     * @throws com.rackspace.idm.exception.NotFoundException
     *                                  if user can not be found
     * @throws com.rackspace.idm.exception.SaveOrUpdateException if there was a problem storing the information
     */
    MobilePhone addPhoneToUser(String userId, Phonenumber.PhoneNumber phoneNumber);

    /**
     * Sends the verification pin to the specified mobile phone and stores the information in some manner in which
     * the {@link #verifyPhoneForUser} can be used to verify the user received the pin. If this method is called
     * repeatedly for the same mobilePhone and user, it is up to implementations to determine whether previous PIN
     * codes
     * are valid. Implementations can also determine for how long each PIN is valid (e.g. - 10 minutes after issuance)
     *
     * @param userId
     * @param mobilePhoneId
     * @throws com.rackspace.idm.exception.NotFoundException if specified user or phone can not be found
     * @throws com.rackspace.idm.exception.MultiFactorDeviceNotAssociatedWithUserException If specified device not associated with user
     * @throws com.rackspace.idm.exception.MultiFactorDeviceAlreadyVerifiedException if the specified phone is already verified for this user
     * @throws com.rackspace.identity.multifactor.exceptions.InvalidPhoneNumberException if the phone number associated with the mobilePhoneId is invalid for some reason
     * @throws com.rackspace.identity.multifactor.exceptions.SendPinException if there was a problem sending the pin to the phone
     * @throws com.rackspace.idm.exception.SaveOrUpdateException if there was a problem storing the information
     */
    void sendVerificationPin(String userId, String mobilePhoneId);

    /**
     * Works in concert with {@link #sendVerificationPin}. Verifies that the pin provided is the pin previously sent to
     * the mobilePhone via a {@link #sendVerificationPin} call.
     * Implementations determine the busines rules associated with pin verification (e.g. how long each PIN is valid
     * after issuance, whether multiple pins are supported at the
     * same time, etc)
     *
     * @param userId
     * @param mobilePhoneId
     * @param pin
     * @throws com.rackspace.idm.exception.MultiFactorDeviceNotAssociatedWithUserException If specified device not associated with user
     * @throws com.rackspace.idm.exception.MultiFactorDevicePinValidationException if the provided pin does not match or is expired
     * @throws com.rackspace.idm.exception.SaveOrUpdateException if there was a problem storing the information
     */
    void verifyPhoneForUser(String userId, String mobilePhoneId, Pin pin);

    /**
     * Retrieve the specified phone associated with the user.
     * @param user
     * @param mobilePhoneId
     * @return
     *
     * @throws com.rackspace.idm.exception.NotFoundException if the specified phone does not exist or is not associated with the user
     */
    MobilePhone checkAndGetMobilePhoneFromUser(User user, String mobilePhoneId);


    /**
     * Deletes the mobile phone associated with the user. The phone can only be deleted in the following
     * circumstances:
     *
     * <ul>
     *     <li>The user has multi-factor type set to [OTP] for their profile</li>
     *     <li>The user has multi-factor authentication [Disabled] for their profile</li>
     * </ul>
     *
     * @param user
     * @param mobilePhoneId
     * @return
     */
    void deleteMobilePhoneFromUser(User user, String mobilePhoneId);

    /**
     * Updates the account's multifactor settings. Must verify that the account is properly configured to enable
     * multi-factor (e.g. - has a verified device).
     *
     * @param userId
     *
     * @throws IllegalStateException if the account is not in a valid state to enable multi-factor
     */
    void updateMultiFactorSettings(String userId, MultiFactor multiFactor);


    /**
     * Removes multifactor from the user's account. Does not need to delete unused phones at this time, but must remove any
     * link between the user and phone.
     *
     * @param userId
     */
    void removeMultiFactorForUser(String userId);

    /**
     * Sends an SMS passcode to the specified user.
     *
     * @param userId
     * @throws com.rackspace.idm.exception.NotFoundException if the user does not exist
     * @throws com.rackspace.identity.multifactor.exceptions.TelephonyDisabledException if sending of sms messages is disabled
     * @throws com.rackspace.idm.exception.MultiFactorNotEnabledException if the user does not have multifactor enabled or is incorrectly set up
     *
     */
    void sendSmsPasscode(String userId);

    /**
     * Verifies that the specified passcode is the same passcode as that sent/received by the user through other means (e.g an SMS message)
     *
     * @param userId
     * @param passcode
     * @return
     * @throws com.rackspace.idm.exception.NotFoundException if the user does not exist
     * @throws com.rackspace.idm.exception.MultiFactorNotEnabledException if the user does not have multifactor enabled or is incorrectly set up
     */
    MfaAuthenticationResponse verifyPasscode(String userId, String passcode);

     /**
     * Return list of mobilephones associated with a given user or empty list if not found
     *
     * @param user
     * @return
     */
    List<MobilePhone> getMobilePhonesForUser(User user);

    /**
     * Return one bypass code, giving a duration in seconds, associated with a given user
     *
     * @param user
     * @param validSecs
     * @return
     */
    public String getBypassCode(User user, Integer validSecs);

    /**
     * Return list of bypass codes (self-service), giving a duration in seconds, associated with a given user
     *
     * @param user
     * @param validSecs <code>null</code> can be used to define "never expires"
     * @return
     */
    public List<String> getSelfServiceBypassCodes(User user, Integer validSecs, BigInteger numberOfCodes);

    /**
     * Update the multifactor settings on a domain
     *
     * @param domainId
     * @param multiFactorDomain
     *
     * @throws com.rackspace.idm.exception.NotFoundException If domainId does not exist
     */
    void updateMultiFactorDomainSettings(String domainId, MultiFactorDomain multiFactorDomain);

    /**
     * Whether or not multi-factor services are enabled or not.
     *
     * Returns true if multi-factor status is either FULL or BETA otherwise returns FALSE.
     */
    boolean isMultiFactorEnabled();

    /**
     * Whether or not multi-factor services are enabled and multi-factor beta is disabled.
     *
     * Returns true if multi-factor services are enabled and multi-factor beta is disabled. Returns false otherwise.
     */
    public boolean isMultiFactorGloballyEnabled();

    /**
     * Whether or not multi-factor services are enabled or not for a given user.
     *
     * Returns true if multi-factor status is FULL, true if multi-factor status is BETA and user has
     * multi-factor beta role, and false otherwise.
     */
    boolean isMultiFactorEnabledForUser(BaseUser user);

    /**
     * Whether or not multi-factor devices exist for a given user.
     * @param user
     * @return
     */
    boolean userHasMultiFactorDevices(BaseUser user);

    /**
     * Whether or not multi-factor verified devices exist for a given user.
     * @param user
     * @return
     */
    boolean userHasVerifiedMultiFactorDevices(BaseUser user);

    /**
     * Adds an OTP device to the user.
     */
    OTPDevice addOTPDeviceToUser(String userId, String name);

    /**
     * Get a list of OTP devices, regardless of status, associated with the user
     *
     * @param user
     * @return
     */
    List<OTPDevice> getOTPDevicesForUser(User user);

    /**
     * Gets checks an OTP device from the user by id.
     */
    OTPDevice checkAndGetOTPDeviceFromUserById(String userId, String deviceId);

    /**
     * Returns the OTPDevice specified by the deviceId that is associated with the specified user. Returns null if
     * <ul>
     * <li>The deviceId exists, but is not associated with the specified user</li>
     * <li>The deviceId does not exist</li>
     * <li>The user does not exist</li>
     * </ul>
     *
     * @param userId
     * @param deviceId
     * @return
     */
    OTPDevice getOTPDeviceFromUserById(String userId, String deviceId);

    /**
     * Verifies an OTP device for the user giving the device id and the verification code.
     */
    void verifyOTPDeviceForUserById(String userId, String deviceId, String verificationCode);

    /**
     * Deletes the OTP device associated with the user specified by userId. The device can only be deleted in the following
     * circumstances:
     *
     * <ul>
     *     <li>The user has another mobile passcode (OTP) device associated to their profile</li>
     *     <li>The user has multi-factor type set to [SMS text passcode] for their profile</li>
     *     <li>The user has multi-factor authentication [Disabled] for their profile</li>
     *     <li>The device has not been verified</li>
     * </ul>
     *
     * @param userId
     * @param deviceId
     * @return
     */
    void deleteOTPDeviceForUser(String userId, String deviceId);

    /**
     * Gets the type of the multifactor device used by this user.
     */
    FactorTypeEnum getMultiFactorType(BaseUser user);

    /**
     * Checks if the multifactor device used by this user is a phone.
     */
    boolean isMultiFactorTypePhone(BaseUser user);

    /**
     * Checks if the multifactor device user by this user is an OTP device.
     */
    boolean isMultiFactorTypeOTP(BaseUser user);

    /**
     * Get a list of all MFA devices associated with the user
     * @param user
     * @return
     */
    List<MultiFactorDevice> getMultiFactorDevicesForUser(User user);

    /**
     * Get the logical state of the user's multi-factor state (locked vs active).
     *
     * @param user
     * @return
     */
    MultiFactorStateEnum getLogicalUserMultiFactorState(User user);

    /**
     * Is the user locked from a local MFA perspective
     *
     * @param user
     * @return
     */
    boolean isUserLocalLocked(User user);
}
