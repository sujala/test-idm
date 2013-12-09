package com.rackspace.idm.multifactor.service;

import com.google.i18n.phonenumbers.Phonenumber;
import com.rackspace.idm.domain.entity.MobilePhone;
import com.rackspace.idm.exception.InvalidPhoneNumberException;
import com.rackspace.idm.multifactor.domain.Pin;

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
     * Returns the standard string representation used by the multiFactor
     * service (E.123) for the provided phone number (e.g. "+1 512-444-0000" for US numbers).
     *
     * @param phoneNumber
     * @return
     */
    String canonicalizePhoneNumberToString(Phonenumber.PhoneNumber phoneNumber);

    /**
     * Parses the provided string representation into the standard phone number object required for use of the
     * multiFactor service. The country code is assumed to be for United States (1), unless the phone number is
     * provided
     * in international format and starts with a '+' (e.g. 41 44 668 1800' for Switzerland country code (41)).
     *
     * @param phoneNumber
     * @return
     * @throws com.rackspace.idm.exception.InvalidPhoneNumberException
     *
     */
    Phonenumber.PhoneNumber parsePhoneNumber(String phoneNumber) throws InvalidPhoneNumberException;

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
     * @throws InvalidPhoneNumberException if the phone number associated with the mobilePhoneId is invalid for some reason
     * @throws com.rackspace.idm.multifactor.providers.SendPinException if there was a problem sending the pin to the phone
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
     * Return the mobilephone associated with the given mobilePhoneId or null if not found
     *
     * @param mobilePhoneId
     * @return
     */
    MobilePhone getMobilePhoneById(String mobilePhoneId);
}