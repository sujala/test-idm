package com.rackspace.idm.multifactor.service;

import com.google.i18n.phonenumbers.Phonenumber;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.MultiFactor;
import com.rackspace.identity.multifactor.domain.MfaAuthenticationResponse;
import com.rackspace.identity.multifactor.domain.Pin;
import com.rackspace.identity.multifactor.providers.*;
import com.rackspace.identity.multifactor.providers.MobilePhoneVerification;
import com.rackspace.identity.multifactor.providers.ProviderPhone;
import com.rackspace.identity.multifactor.providers.ProviderUser;
import com.rackspace.identity.multifactor.providers.UserManagement;
import com.rackspace.identity.multifactor.providers.duo.domain.DuoPhone;
import com.rackspace.identity.multifactor.providers.duo.domain.DuoUser;
import com.rackspace.identity.multifactor.util.IdmPhoneNumberUtil;
import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperClient;
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperConstants;
import com.rackspace.idm.domain.dao.MobilePhoneDao;
import com.rackspace.idm.domain.entity.MobilePhone;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.*;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;
import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

    private static final String EXTERNAL_PROVIDER_ERROR_FORMAT = "operation={},userId={},username={},externalId={},externalResponse={}";

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
    private final Logger multiFactorConsistencyLogger = LoggerFactory.getLogger(GlobalConstants.MULTIFACTOR_CONSISTENCY_LOG_NAME);

    public static final String CONFIG_PROP_PHONE_MEMBERSHIP_ENABLED = "feature.multifactor.phone.membership.enabled";

    @Autowired
    private UserService userService;

    @Autowired
    private MobilePhoneDao mobilePhoneDao;

    @Autowired
    private MobilePhoneVerification mobilePhoneVerification;

    @Autowired
    private MultiFactorAuthenticationService multiFactorAuthenticationService;

    @Autowired
    private UserManagement<DuoUser, DuoPhone> userManagement;

    @Autowired
    private Configuration globalConfig;

    @Autowired
    private AtomHopperClient atomHopperClient;

    @Autowired
    private ScopeAccessService scopeAccessService;

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
            mobilePhone = createAndLinkMobilePhone(phoneNumber, user);
        } catch (DuplicateException e) {
            mobilePhone = getAndLinkMobilePhone(phoneNumber, user);
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

        MobilePhone phone = mobilePhoneDao.getById(mobilePhoneId);
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
        return mobilePhoneDao.getById(mobilePhoneId);
    }

    @Override
    public void updateMultiFactorSettings(String userId, MultiFactor multiFactor) {
        User user = userService.checkAndGetUserById(userId);

        if (user.isMultiFactorEnabled() == multiFactor.isEnabled()) {
            return; //no-op
        } else if (!StringUtils.hasText(user.getMultiFactorMobilePhoneRsId())) {
            throw new IllegalStateException(ERROR_MSG_NO_DEVICE);
        } else if (!user.isMultiFactorDeviceVerified()) {
            throw new IllegalStateException(ERROR_MSG_NO_VERIFIED_DEVICE);
        }

        if (multiFactor.isEnabled()) {
            enableMultiFactorForUser(user);
        } else {
            disableMultiFactorForUser(user);
        }
    }

    @Override
    public void removeMultiFactorForUser(String userId) {
        User user = userService.checkAndGetUserById(userId);
        String providerUserId = user.getExternalMultiFactorUserId();
        String phoneRsId = user.getMultiFactorMobilePhoneRsId();

        boolean enabled = user.isMultiFactorEnabled();

        //reset user
        user.setMultifactorEnabled(null);
        user.setExternalMultiFactorUserId(null);
        user.setMultiFactorMobilePhoneRsId(null);
        user.setMultiFactorDevicePin(null);
        user.setMultiFactorDeviceVerified(null);
        user.setMultiFactorDevicePinExpiration(null);
        userService.updateUserForMultiFactor(user);

        if (enabled) {
            atomHopperClient.asyncPost(user, AtomHopperConstants.MULTI_FACTOR);
        }

        //unlink phone from user.
        try {
            if (isPhoneUserMembershipEnabled()) {
                //get and unlink phone (if exists)
                MobilePhone phone = null;
                if (StringUtils.hasText(phoneRsId)) {
                    phone = mobilePhoneDao.getById(phoneRsId);
                    phone.removeMember(user);
                    mobilePhoneDao.updateObjectAsIs(phone);
                }
            }
        } catch (Exception e) {
            //eat the exception, but log it. This is a consistency issue where the phone will think some people are using it that are not, but will not cause an error in operation.
            multiFactorConsistencyLogger.error(String.format("Error removing user '%s' from phone '%s' membership. The phone membership will" +
                    "be inconsistent unless this is corrected. The user's DN should be removed from the phone's 'member' attribute.", userId, phoneRsId), e);
        }

        //note - if this fails we will have a orphaned user account in duo that is not linked to anything in ldap since
        //the info in ldap has been removed.
        if (StringUtils.hasText(providerUserId)) {
            deleteExternalUser(user.getId(), user.getUsername(), providerUserId);
        }
    }

    @Override
    public void sendSmsPasscode(String userId) {
        User user = userService.checkAndGetUserById(userId);
        verifyMultiFactorStateOnUser(user);

        MobilePhone phone = mobilePhoneDao.getById(user.getMultiFactorMobilePhoneRsId());
        verifyMultiFactorStateOnPhone(user, phone);

        multiFactorAuthenticationService.sendSmsPasscodeChallenge(user.getExternalMultiFactorUserId(), phone.getExternalMultiFactorPhoneId());
    }

    @Override
    public MfaAuthenticationResponse verifyPasscode(String userId, String passcode) {
        User user = userService.checkAndGetUserById(userId);
        verifyMultiFactorStateOnUser(user);

        MobilePhone phone = mobilePhoneDao.getById(user.getMultiFactorMobilePhoneRsId());
        verifyMultiFactorStateOnPhone(user, phone);

        MfaAuthenticationResponse response = multiFactorAuthenticationService.verifyPasscodeChallenge(user.getExternalMultiFactorUserId(), phone.getExternalMultiFactorPhoneId(), passcode);

        return response;
    }

    private void verifyMultiFactorStateOnUser(User user) {
        if (!user.isMultiFactorEnabled() || !StringUtils.hasText(user.getExternalMultiFactorUserId())
                ||  !StringUtils.hasText(user.getMultiFactorMobilePhoneRsId())) {
            throw new MultiFactorNotEnabledException(String.format("Multi factor is either not enabled or incorrectly set up on account '%s'", user.getId()));
        }
    }

    private void verifyMultiFactorStateOnPhone(User user, MobilePhone phone) {
        if (phone == null || !StringUtils.hasText(phone.getExternalMultiFactorPhoneId())) {
            throw new MultiFactorNotEnabledException(String.format("Multi factor not enabled on account '%s' - invalid phone", user.getId()));
        }
    }

    public List<MobilePhone> getMobilePhonesForUser(User user) {
        Assert.notNull(user);

        ArrayList<MobilePhone> result = new ArrayList<MobilePhone>();

        if (user.getMultiFactorMobilePhoneRsId() == null) {
            return result;
        }

        MobilePhone phone = mobilePhoneDao.getById(user.getMultiFactorMobilePhoneRsId());

        if (phone == null) {
            multiFactorConsistencyLogger.error(String.format("Error retrieving device '%s' for user '%s'.  User contains phone rsId but device does not exist", user.getMultiFactorMobilePhoneRsId(), user.getId()));
        } else {
            result.add(phone);
        }
        return result;
    }

    private void enableMultiFactorForUser(User user) {
        MobilePhone phone = mobilePhoneDao.getById(user.getMultiFactorMobilePhoneRsId());

        DuoPhone duoPhone = new DuoPhone();
        duoPhone.setNumber(phone.getTelephoneNumber());

        DuoUser duoUser = new DuoUser();
        duoUser.setUsername(user.getId());

        ProviderUser providerUser = userManagement.createUser(duoUser);
        user.setExternalMultiFactorUserId(providerUser.getProviderId());

        try {
            ProviderPhone providerPhone = userManagement.linkMobilePhoneToUser(providerUser.getProviderId(), duoPhone);
            phone.setExternalMultiFactorPhoneId(providerPhone.getProviderId());
        } catch (com.rackspace.identity.multifactor.exceptions.NotFoundException e) {
            //translate to IDM not found exception. Thrown if user does not exist in duo, though it should since was created
            //above
            multiFactorConsistencyLogger.error(String.format("An error occurred enabling multifactor for user '%s'. Duo returned a NotFoundException for the user's duo profile '%s'.", user.getId(), providerUser.getProviderId()), e);
            throw new NotFoundException(e.getMessage(), e);
        }
        mobilePhoneDao.updateObjectAsIs(phone);

        boolean alreadyEnabled = user.isMultiFactorEnabled();

        user.setMultifactorEnabled(true);
        userService.updateUserForMultiFactor(user);

        if (!alreadyEnabled) {
            scopeAccessService.expireAllTokensForUserById(user.getId());
            atomHopperClient.asyncPost(user, AtomHopperConstants.MULTI_FACTOR);
        }
    }

    private void disableMultiFactorForUser(User user) {
        String providerUserId = user.getExternalMultiFactorUserId();

        boolean enabled = user.isMultiFactorEnabled();

        user.setMultifactorEnabled(false);
        user.setExternalMultiFactorUserId(null);
        userService.updateUserForMultiFactor(user);

        if (enabled){
            atomHopperClient.asyncPost(user, AtomHopperConstants.MULTI_FACTOR);
        }

        deleteExternalUser(user.getId(), user.getUsername(), providerUserId);
    }

    private void deleteExternalUser(String userId, String username, String externalProviderUserId) {
        //note - if this fails we will have a orphaned user account in duo that is not linked to anything in ldap since
        //the info in ldap has been removed.
        if (StringUtils.hasText(externalProviderUserId)) {
            try {
                userManagement.deleteUserById(externalProviderUserId);
            } catch (Exception e) {
                //if there was ANY exception raised delete the user from the 3rd party provider, we must log it as the
                //user's info _may_ be left in the 3rd party, but we will not link to it from ldap. Manual cleanup will
                //be required
                LOG.error(String.format("Error encountered removing user's multifactor profile from third party. username: '%s'; external providerId: '%s'. Encountered error '%s'", username, externalProviderUserId, e.getMessage()));
                multiFactorConsistencyLogger.error(EXTERNAL_PROVIDER_ERROR_FORMAT,
                        new Object[]{"DELETE USER", userId, username, externalProviderUserId, e.getMessage()});

            }
        }
    }

    /**
     * Creates a new mobilePhone entry in CA
     *
     * @param phoneNumber
     * @return
     * @throws DuplicateException If the phone already exists
     */
    private MobilePhone createAndLinkMobilePhone(Phonenumber.PhoneNumber phoneNumber, User user) {
        Assert.notNull(phoneNumber);
        String canonicalizedPhone = IdmPhoneNumberUtil.getInstance().canonicalizePhoneNumberToString(phoneNumber);

        MobilePhone mobilePhone = new MobilePhone();
        mobilePhone.setTelephoneNumberAndCn(canonicalizedPhone);
        mobilePhone.setId(mobilePhoneDao.getNextId());

        if (isPhoneUserMembershipEnabled()) {
            try {
                mobilePhone.addMember(user);
            } catch (Exception e) {
                multiFactorConsistencyLogger.error(String.format("Error adding user '%s' to phone '%s' membership. The phone membership will" +
                        "be inconsistent unless this is corrected. The user's DN should be added to the phone's 'member' attribute.", user.getId(), mobilePhone.getId()), e);
            }
        }

        mobilePhoneDao.addObject(mobilePhone);
        return mobilePhone;
    }

    private MobilePhone getAndLinkMobilePhone(Phonenumber.PhoneNumber phoneNumber, User user) {
        Assert.notNull(phoneNumber);
        String canonicalizedPhone = IdmPhoneNumberUtil.getInstance().canonicalizePhoneNumberToString(phoneNumber);

        MobilePhone mobilePhone = mobilePhoneDao.getByTelephoneNumber(IdmPhoneNumberUtil.getInstance().canonicalizePhoneNumberToString(phoneNumber));
        if (mobilePhone == null) {
            throw new IllegalStateException(String.format("Mobile phone '%s' could not be found", canonicalizedPhone));
        }

        if (isPhoneUserMembershipEnabled()) {
            try {
                mobilePhone.addMember(user);
                mobilePhoneDao.updateObjectAsIs(mobilePhone);
            } catch (Exception e) {
                multiFactorConsistencyLogger.error(String.format("Error adding user '%s' to phone '%s' membership. The phone membership will" +
                        "be inconsistent unless this is corrected. The user's DN should be added to the phone's 'member' attribute.", user.getId(), mobilePhone.getId()), e);
            }
        }

        return mobilePhone;
    }

    private Integer getPinValidityInMinutes() {
        return globalConfig.getInt(PIN_VALIDITY_LENGTH_PROP_NAME, PIN_VALIDITY_LENGTH_DEFAULT_VALUE);
    }

    private DateTime generatePinExpirationDateFromNow() {
        return new DateTime().plusMinutes(getPinValidityInMinutes());
    }

    public boolean isPhoneUserMembershipEnabled() {
        return globalConfig.getBoolean(CONFIG_PROP_PHONE_MEMBERSHIP_ENABLED, false);
    }
}
