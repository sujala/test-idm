package com.rackspace.idm.multifactor.util;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.rackspace.idm.domain.entity.MobilePhone;
import com.rackspace.idm.exception.InvalidPhoneNumberException;

public final class IdmPhoneNumberUtil {

    private IdmPhoneNumberUtil() {}

    /**
     * Singleton instance to parse and format phone numbers
     */
    private static PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();

    /**
     * Returns the standard string representation used by the multiFactor
     * service (E.123) for the provided phone number (e.g. "+1 512-444-0000" for US numbers).
     *
     * @param phoneNumber
     * @return
     */
    public static String canonicalizePhoneNumberToString(Phonenumber.PhoneNumber phoneNumber) {
        return phoneNumberUtil.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL);

    }

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
    public static Phonenumber.PhoneNumber parsePhoneNumber(String phoneNumber) throws InvalidPhoneNumberException {
        try {
            return phoneNumberUtil.parse(phoneNumber, MobilePhone.TELEPHONE_DEFAULT_REGION);
        } catch (NumberParseException e) {
            throw new InvalidPhoneNumberException("The phone number '" + phoneNumber + "' does not appear to be a valid phone number", e);
        }

    }
}
