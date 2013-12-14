package com.rackspace.idm.multifactor.service;

import com.google.i18n.phonenumbers.Phonenumber;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.MultiFactorSettings;
import com.rackspace.idm.domain.dao.impl.LdapMobilePhoneRepository;
import com.rackspace.idm.domain.entity.MobilePhone;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.*;
import com.rackspace.idm.multifactor.domain.Pin;
import com.rackspace.idm.multifactor.providers.MobilePhoneVerification;
import com.rackspace.idm.multifactor.providers.ProviderPhone;
import com.rackspace.idm.multifactor.providers.ProviderUser;
import com.rackspace.idm.multifactor.providers.UserManagement;
import com.rackspace.idm.multifactor.util.IdmPhoneNumberUtil;
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

    public static final String ERROR_MSG_NO_DEVICE = "User not associated with a multifactor device";
    public static final String ERROR_MSG_NO_VERIFIED_DEVICE = "Device not verified";

    @Autowired
    private UserService userService;

    @Autowired
    private LdapMobilePhoneRepository mobilePhoneRepository;

    @Autowired
    private MobilePhoneVerification mobilePhoneVerification;

    @Autowired
    private UserManagement userManagement;

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
            mobilePhone = mobilePhoneRepository.getByTelephoneNumber(IdmPhoneNumberUtil.getInstance().canonicalizePhoneNumberToString(phoneNumber));
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

        Phonenumber.PhoneNumber phoneNumber = IdmPhoneNumberUtil.getInstance().parsePhoneNumber(phone.getTelephoneNumber());
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

    @Override
    public void updateMultiFactorSettings(String userId, MultiFactorSettings multiFactorSettings) {
        User user = userService.checkAndGetUserById(userId);

        if (user.isMultiFactorEnabled() == multiFactorSettings.isEnabled()) {
            return; //no-op
        } else if (!StringUtils.hasText(user.getMultiFactorMobilePhoneRsId())) {
            throw new IllegalStateException(ERROR_MSG_NO_DEVICE);
        } else if (!user.isMultiFactorDeviceVerified()) {
            throw new IllegalStateException(ERROR_MSG_NO_VERIFIED_DEVICE);
        }

        if (multiFactorSettings.isEnabled()) {
            enableMultiFactorForUser(user);
        } else {
            disableMultiFactorForUser(user);
        }
    }

    @Override
    public void removeMultiFactorForUser(String userId) {
        User user = userService.checkAndGetUserById(userId);

        String providerUserId = user.getExternalMultiFactorUserId();

        user.setMultifactorEnabled(null);
        user.setExternalMultiFactorUserId(null);
        user.setMultiFactorMobilePhoneRsId(null);
        user.setMultiFactorDevicePin(null);
        user.setMultiFactorDeviceVerified(null);
        user.setMultiFactorDevicePinExpiration(null);
        userService.updateUserForMultiFactor(user);

        //note - if this fails we will have a orphaned user account in duo that is not linked to anything in ldap since
        //the info in ldap has been removed.
        if (StringUtils.hasText(providerUserId)) {
            userManagement.deleteUserById(providerUserId);
        }
    }

    private void enableMultiFactorForUser(User user) {
        MobilePhone phone = mobilePhoneRepository.getById(user.getMultiFactorMobilePhoneRsId());

        ProviderUser providerUser = userManagement.createUser(user);
        user.setExternalMultiFactorUserId(providerUser.getProviderId());

        ProviderPhone providerPhone = userManagement.linkMobilePhoneToUser(providerUser.getProviderId(), phone);
        phone.setExternalMultiFactorPhoneId(providerPhone.getProviderId());
        mobilePhoneRepository.updateObjectAsIs(phone);

        user.setMultifactorEnabled(true);
        userService.updateUserForMultiFactor(user);
    }

    private void disableMultiFactorForUser(User user) {
        String providerUserId = user.getExternalMultiFactorUserId();

        user.setMultifactorEnabled(false);
        user.setExternalMultiFactorUserId(null);
        userService.updateUserForMultiFactor(user);

        //note - if this fails we will have a orphaned user account in duo that is not linked to anything in ldap since
        //the info in ldap has been removed.
        if (StringUtils.hasText(providerUserId)) {
            //remove the account from duo
            userManagement.deleteUserById(providerUserId);
        }
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
        String canonicalizedPhone = IdmPhoneNumberUtil.getInstance().canonicalizePhoneNumberToString(phoneNumber);

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
