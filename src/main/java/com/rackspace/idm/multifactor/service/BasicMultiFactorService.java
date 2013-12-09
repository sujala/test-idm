package com.rackspace.idm.multifactor.service;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.rackspace.idm.domain.dao.impl.LdapMobilePhoneRepository;
import com.rackspace.idm.domain.entity.MobilePhone;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.InvalidPhoneNumberException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.exception.SaveOrUpdateException;
import com.rackspace.idm.exception.*;
import com.rackspace.idm.multifactor.domain.Pin;
import com.rackspace.idm.multifactor.providers.MobilePhoneVerification;
import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.Date;

/**
 * Simple multi-factor implementation that stores mobile phones in the ldap directory, and integrates with a single
 * external provider. If the external provider
 * is not available the services will fail.
 */
@Component
public class BasicMultiFactorService implements MultiFactorService {
    private static final Logger LOG = LoggerFactory.getLogger(BasicMultiFactorService.class);
    public static final String ERROR_MSG_SAVE_OR_UPDATE_USER = "Error updating user %s";
    public static final String ERROR_MSG_PHONE_NOT_ASSOCIATED_WITH_USER = "Specified phone is not associated with user";

    @Autowired
    private UserService userService;

    @Autowired
    private LdapMobilePhoneRepository mobilePhoneRepository;

    @Autowired
    private MobilePhoneVerification mobilePhoneVerification;

    @Autowired
    private Configuration globalConfig;

    /**
     * Name of property in standard IDM property file that specifies for how many minutes a verification "pin" code is
     * valid
     */
    public static final String PIN_VALIDITY_LENGTH_PROP_NAME = "duo.security.verify.pin.validity.length";

    /**
     * Default value for how long a pin is valid for if a property value is not provided.
     */
    public static final int PIN_VALIDITY_LENGTH_DEFAULT_VALUE = 10;

    /**
     * Singleton instance to parse and format phone numbers
     */
    private PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();

    @Override
    public MobilePhone addPhoneToUser(String userId, Phonenumber.PhoneNumber phoneNumber) {
        Assert.notNull(phoneNumber);
        Assert.notNull(userId);

        User user = userService.checkAndGetUserById(userId);
        if (user.getMultiFactorMobilePhoneRsId() != null) {
            String errMsg = "User is already associated with a mobile phone";
            LOG.warn(errMsg);
            throw new IllegalStateException(errMsg);
        }

        MobilePhone mobilePhone = null;
        try {
            mobilePhone = createMobilePhone(phoneNumber);
        } catch (DuplicateException e) {
            //phone number exists already, retrieve it to link user to it
            mobilePhone = mobilePhoneRepository.getByTelephoneNumber(canonicalizePhoneNumberToString(phoneNumber));
            if (mobilePhone == null) {
                throw new IllegalStateException("Mobile phone exists but could not be found");
            }
        } catch (Exception ex) {
            throw new SaveOrUpdateException("Error creating mobile phone", ex);
        }

        //now link user to the created/retrieved phone
        user.setMultiFactorMobilePhoneRsId(mobilePhone.getId());
        userService.updateUserForMultiFactor(user);

        return mobilePhone;
    }

    @Override
    public String canonicalizePhoneNumberToString(Phonenumber.PhoneNumber phoneNumber) {
        return phoneNumberUtil.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL);
    }

    @Override
    public Phonenumber.PhoneNumber parsePhoneNumber(String phoneNumber) {
        try {
            return phoneNumberUtil.parse(phoneNumber, MobilePhone.TELEPHONE_DEFAULT_REGION);
        } catch (NumberParseException e) {
            throw new InvalidPhoneNumberException("The phone number '" + phoneNumber + "' does not appear to be a valid phone number", e);
        }
    }

    @Override
    public void sendVerificationPin(String userId, String mobilePhoneId) {
        Assert.notNull(userId);
        Assert.notNull(mobilePhoneId);

        User currentUser = userService.checkAndGetUserById(userId);
        if (!mobilePhoneId.equals(currentUser.getMultiFactorMobilePhoneRsId())) {
            throw new MultiFactorDeviceNotAssociatedWithUserException(ERROR_MSG_PHONE_NOT_ASSOCIATED_WITH_USER);
        }

        MobilePhone phone = mobilePhoneRepository.getById(mobilePhoneId);
        if (phone == null) {
            throw new NotFoundException("Phone not found");
        } else if (currentUser.getMultiFactorDeviceVerified() != null && currentUser.getMultiFactorDeviceVerified()) {
            throw new MultiFactorDeviceAlreadyVerifiedException("Device already verified");
        }

        Phonenumber.PhoneNumber phoneNumber = parsePhoneNumber(phone.getTelephoneNumber());
        Pin pinSent = mobilePhoneVerification.sendPin(phoneNumber);

        //expiration date
        Date expiration = generatePinExpirationDateFromNow().toDate();
        currentUser.setMultiFactorDevicePin(pinSent.getPin());
        currentUser.setMultiFactorDevicePinExpiration(expiration);
        userService.updateUserForMultiFactor(currentUser);
    }

    @Override
    public void verifyPhoneForUser(String userId, String mobilePhoneId, Pin pin) {
        Assert.notNull(userId);
        Assert.notNull(mobilePhoneId);
        Assert.notNull(pin);

        User currentUser = userService.checkAndGetUserById(userId);

        if (!StringUtils.hasText(currentUser.getMultiFactorMobilePhoneRsId()) || !currentUser.getMultiFactorMobilePhoneRsId().equals(mobilePhoneId)) {
            throw new MultiFactorDeviceNotAssociatedWithUserException("User not associated with mobile phone");
        } else if (pin.getPin() == null || !pin.getPin().equals(currentUser.getMultiFactorDevicePin())) {
            throw new MultiFactorDevicePinValidationException("Pin does not match");
        } else if (currentUser.getMultiFactorDevicePinExpiration() == null || currentUser.getMultiFactorDevicePinExpiration().before(new Date())) {
            throw new MultiFactorDevicePinValidationException("Pin is expired");
        }

        //pin has been verified
        currentUser.setMultiFactorDevicePinExpiration(null);
        currentUser.setMultiFactorDevicePin(null);
        currentUser.setMultiFactorDeviceVerified(true);
        currentUser.setMultifactorEnabled(false);
        userService.updateUserForMultiFactor(currentUser);
    }

    @Override
    public MobilePhone getMobilePhoneById(String mobilePhoneId) {
        return mobilePhoneRepository.getById(mobilePhoneId);
    }

    /**
     * Creates a new mobilePhone entry in CA
     *
     * @param phoneNumber
     * @return
     * @throws DuplicateException If the phone already exists
     */
    private MobilePhone createMobilePhone(Phonenumber.PhoneNumber phoneNumber) {
        Assert.notNull(phoneNumber);
        String canonicalizedPhone = canonicalizePhoneNumberToString(phoneNumber);

        MobilePhone mobilePhone = new MobilePhone();
        mobilePhone.setTelephoneNumber(canonicalizedPhone);
        mobilePhone.setId(mobilePhoneRepository.getNextId());

        mobilePhoneRepository.addObject(mobilePhone);
        return mobilePhone;
    }

    private Integer getPinValidityInMinutes() {
        return globalConfig.getInt(PIN_VALIDITY_LENGTH_PROP_NAME, PIN_VALIDITY_LENGTH_DEFAULT_VALUE);
    }

    private DateTime generatePinExpirationDateFromNow() {
        return new DateTime().plusMinutes(getPinValidityInMinutes());
    }
}
