package com.rackspace.idm.multifactor.service;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.Phonenumber;
import com.rackspace.idm.domain.entity.MobilePhone;
import com.rackspace.idm.exception.InvalidPhoneNumberException;

/**
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
     */
    MobilePhone addPhoneToUser(String userId, Phonenumber.PhoneNumber phoneNumber);

    /**
     * Returns the standard string representation used by the multiFactor
     * service (E.123) for the provided phone number (e.g. "+1 512-444-0000").
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
}