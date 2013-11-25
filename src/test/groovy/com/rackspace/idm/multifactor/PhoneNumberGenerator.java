package com.rackspace.idm.multifactor;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import org.apache.commons.lang.StringUtils;

import java.util.Random;

/**
 */
public final class PhoneNumberGenerator {
    private static PhoneNumberGenerator instance = new PhoneNumberGenerator();

    private PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
    private String exampleNumber;
    private String exampleNumberPrefix = null;

    private Random ran = new Random();
    private final int MAX_RANDOM_NUM = 9999;
    private final int RANDOM_BIT_LENGTH = String.valueOf(MAX_RANDOM_NUM).length();

    private PhoneNumberGenerator() {
        String exampleNumber = internalCanonicalizePhoneNumberToString(phoneNumberUtil.getExampleNumber("US"));
        exampleNumberPrefix = exampleNumber.substring(0, exampleNumber.length() - RANDOM_BIT_LENGTH);
    }

    public static PhoneNumberGenerator getInstance() {
        return instance;
    }

    public static Phonenumber.PhoneNumber randomUSNumber() {
        return getInstance().internalRandomUSNumber();
    }

    public static String randomUSNumberAsString() {
        return canonicalizePhoneNumberToString(randomUSNumber());
    }

    public static String canonicalizePhoneNumberToString(Phonenumber.PhoneNumber phoneNumber) {
        return getInstance().internalCanonicalizePhoneNumberToString(phoneNumber);
    }

    private Phonenumber.PhoneNumber internalRandomUSNumber() {
        try {
            Phonenumber.PhoneNumber number = phoneNumberUtil.parse(exampleNumberPrefix + getPaddedRandomInt(), "US");

            //ensure it's "valid"
            if (!phoneNumberUtil.isValidNumber(number)) {
                throw new RuntimeException("Generated phone number is not valid");
            }

            return number;
        } catch (NumberParseException e) {
            throw new RuntimeException("Error generating random phone number");
        }
    }

    private String getPaddedRandomInt() {
        return StringUtils.leftPad(String.valueOf(ran.nextInt(MAX_RANDOM_NUM)), RANDOM_BIT_LENGTH, "0");
    }

    public String internalCanonicalizePhoneNumberToString(Phonenumber.PhoneNumber phoneNumber) {
        return phoneNumberUtil.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL);
    }

}
