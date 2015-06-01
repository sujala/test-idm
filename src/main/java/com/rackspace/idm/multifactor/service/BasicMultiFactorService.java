package com.rackspace.idm.multifactor.service;

import com.google.i18n.phonenumbers.Phonenumber;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.*;
import com.rackspace.identity.multifactor.domain.*;
import com.rackspace.identity.multifactor.providers.*;
import com.rackspace.identity.multifactor.providers.duo.domain.DuoPhone;
import com.rackspace.identity.multifactor.providers.duo.domain.DuoUser;
import com.rackspace.identity.multifactor.util.IdmPhoneNumberUtil;
import com.rackspace.idm.ErrorCodes;
import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperClient;
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperConstants;
import com.rackspace.idm.api.resource.cloud.email.EmailClient;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.BypassDeviceDao;
import com.rackspace.idm.domain.dao.MobilePhoneDao;
import com.rackspace.idm.domain.dao.OTPDeviceDao;
import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.dozer.converters.MultiFactorStateConverter;
import com.rackspace.idm.domain.dozer.converters.UserMultiFactorEnforcementLevelConverter;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.entity.BypassDevice;
import com.rackspace.idm.domain.entity.Domain;
import com.rackspace.idm.domain.entity.MobilePhone;
import com.rackspace.idm.domain.entity.MultiFactorDevice;
import com.rackspace.idm.domain.entity.OTPDevice;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.*;
import com.rackspace.idm.util.BypassDeviceCreationResult;
import com.rackspace.idm.util.BypassHelper;
import com.rackspace.idm.util.OTPHelper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.math.BigInteger;
import java.util.*;

/**
 * Simple multi-factor implementation that stores mobile phones in the ldap directory, and integrates with a single
 * external provider. If the external provider
 * is not available the services will fail.
 */
@Component
public class BasicMultiFactorService implements MultiFactorService {
    private static final Logger LOG = LoggerFactory.getLogger(BasicMultiFactorService.class);

    public static final String ERROR_MSG_PHONE_NOT_ASSOCIATED_WITH_USER = "Specified phone is not associated with user";

    public static final String ERROR_MSG_NO_DEVICE = "User not associated with a multifactor device";
    public static final String ERROR_MSG_NO_VERIFIED_DEVICE = "Device not verified";

    private static final String EXTERNAL_PROVIDER_ERROR_FORMAT = "operation={},userId={},username={},externalId={},externalResponse={}";
    public static final String DELETE_OTP_DEVICE_REQUEST_INVALID_MSG = "You can not delete the last verified OTP device when your account is configured to use OTP multifactor.";

    private final Logger multiFactorConsistencyLogger = LoggerFactory.getLogger(GlobalConstants.MULTIFACTOR_CONSISTENCY_LOG_NAME);

    public static final String CONFIG_PROP_PHONE_MEMBERSHIP_ENABLED = "feature.multifactor.phone.membership.enabled";

    public static final String MULTI_FACTOR_STATE_ACTIVE = "ACTIVE";
    public static final String MULTI_FACTOR_STATE_LOCKED = "LOCKED";

    private static final String PIN_DOES_NOT_MATCH = "Pin does not match";
    private static final String DEVICE_ALREADY_VERIFIED = "Device already verified";

    public static final String MAX_OTP_DEVICES_REACHED = "You have added the maximum number of devices allowed for your profile";

    private final UserMultiFactorEnforcementLevelConverter userMultiFactorEnforcementLevelConverter = new UserMultiFactorEnforcementLevelConverter();

    private MultiFactorStateConverter multiFactorStateConverter = new MultiFactorStateConverter();

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
    private IdentityConfig identityConfig;

    @Lazy
    @Autowired
    private AtomHopperClient atomHopperClient;

    @Autowired
    private ScopeAccessService scopeAccessService;

    @Autowired
    private EmailClient emailClient;

    @Autowired
    private DomainService domainService;

    @Autowired
    private IdentityUserService identityUserService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private TokenRevocationService tokenRevocationService;

    @Autowired
    private OTPDeviceDao otpDeviceDao;

    @Autowired
    private OTPHelper otpHelper;

    @Autowired
    private BypassHelper bypassHelper;

    @Autowired
    private BypassDeviceDao bypassDeviceDao;

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
            // The user currently has a device on their account
            if(user.isMultiFactorEnabled() && isMultiFactorTypePhone(user)) {
                // The user currently has mfa enabled, return an error.
                // If we replace the device now the user would have mfa enabled with an unvalidated phone
                String errMsg = "Cannot replace device with multi-factor enabled for SMS";
                LOG.warn(errMsg);
                throw new IllegalStateException(errMsg);
            } else {
                return replacePhoneOnUser(user, phoneNumber);
            }
        } else {
            return linkPhoneToUser(phoneNumber, user);
        }

    }

    /**
     * Replace the device on the user, resetting all verification of the device on the user. If the phone is already linked
     * to the user, this is a NO-OP and just returns the existing device.
     * @param user
     * @param phoneNumber
     */
    private MobilePhone replacePhoneOnUser(User user, Phonenumber.PhoneNumber phoneNumber) {
        MobilePhone oldPhone = null;
        MobilePhone newPhone = null;

        String oldPhoneId = user.getMultiFactorMobilePhoneRsId();
        if (org.apache.commons.lang.StringUtils.isNotBlank(oldPhoneId)) {
            oldPhone = mobilePhoneDao.getById(oldPhoneId);
        }

        //are we linking to a new phone or an existing?
        if (oldPhone != null && oldPhone.getTelephoneNumber().equals(IdmPhoneNumberUtil.getInstance().canonicalizePhoneNumberToString(phoneNumber)) ) {
            //user is adding same phone number, so this is a no-op. Just return existing phone.
            LOG.debug(String.format("User %s is adding same phone number %s. Ignoring", user.getId(), phoneNumber));
            newPhone = oldPhone;
        } else {
            //create/link new phone (this updates the user state)
            newPhone = linkPhoneToUser(phoneNumber, user);

            /*
            Clean up the old phone if it exists. Double check that it's not the same as the newPhone just to be paranoid.
            If for some reason this fails, not really a big deal.
             */
            if (oldPhone != null && !oldPhone.getId().equals(newPhone.getId())) {
                unlinkPhoneFromUser(oldPhone, user);
            }
        }
        return newPhone;
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
            throw new MultiFactorDeviceAlreadyVerifiedException(DEVICE_ALREADY_VERIFIED);
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
        userService.updateUserForMultiFactor(currentUser);
    }

    @Override
    public MobilePhone getMobilePhoneById(String mobilePhoneId) {
        return mobilePhoneDao.getById(mobilePhoneId);
    }

    @Override
    public void updateMultiFactorSettings(String userId, MultiFactor multiFactor) {
        final User user = userService.checkAndGetUserById(userId);

        if (Boolean.TRUE.equals(multiFactor.isEnabled()) ||
                (multiFactor.getFactorType() != null && Boolean.TRUE.equals(user.isMultiFactorEnabled()))) {
            // Validate for enable multi-factor or change factor type
            if (multiFactor.isUnlock() != null
                    || multiFactor.getUserMultiFactorEnforcementLevel() != null) {
                throw new BadRequestException("Cannot change other settings while setting multi factor.");
            }

            // Change multi-factor type
            final int verifiedOTPDevicesCount = otpDeviceDao.countVerifiedOTPDevicesByParent(user);
            boolean changed = handleMultiFactorType(user, multiFactor, verifiedOTPDevicesCount);

            // Enable multi-factor
            if (Boolean.TRUE.equals(multiFactor.isEnabled())) {
                changed |= handleMultiFactorEnable(user, multiFactor, verifiedOTPDevicesCount);
            }
            if (changed) {
                userService.updateUserForMultiFactor(user);
            }

        } else if (Boolean.FALSE.equals(multiFactor.isEnabled())) {
            // Validate for disable multi-factor
            if (multiFactor.isUnlock() != null
                    || multiFactor.getUserMultiFactorEnforcementLevel() != null
                    || multiFactor.getFactorType() != null) {
                throw new BadRequestException("Cannot change other settings while disabling multi factor.");
            }

            // Disable multi-factor
            if (user.isMultiFactorEnabled()) {
                disableMultiFactorForUser(user);
            }

        } else if (Boolean.TRUE.equals(multiFactor.isUnlock())) {
            // Validate for unlock multi-factor
            if (multiFactor.isEnabled() != null
                    || multiFactor.getUserMultiFactorEnforcementLevel() != null
                    || multiFactor.getFactorType() != null) {
                throw new BadRequestException("Cannot change other settings while unlocking multi factor.");
            }

            // Unlock multi-factor
            handleMultiFactorUnlock(user, multiFactor);

        } else if (multiFactor.getUserMultiFactorEnforcementLevel() != null) {
            // Validate for setting user enforcement for multi-factor
            if (multiFactor.isEnabled() != null
                    || multiFactor.isUnlock() != null
                    || multiFactor.getFactorType() != null) {
                throw new BadRequestException("Cannot change other settings while setting user enforcement level.");
            }

            // Setting user enforcement for multi-factor
            handleMultiFactorUserEnforcementLevel(user, multiFactor);
        }
    }

    private void handleMultiFactorUnlock(User user, MultiFactor multiFactor) {
        if (Boolean.TRUE.equals(multiFactor.isUnlock())) {
            //want to try and unlock user regardless of mfa enable/disable
            if (!identityConfig.getReloadableConfig().getFeatureMultifactorLockingEnabled()
                    && StringUtils.hasText(user.getExternalMultiFactorUserId())) {
                //duo locking is being used, so need to unlock from Duo
                userManagement.unlockUser(user.getExternalMultiFactorUserId());
            }
            setMultiFactorUnlockState(user);
            userService.updateUserForMultiFactor(user);
        }
    }

    private void setMultiFactorUnlockState(User user) {
        user.setMultiFactorState(MULTI_FACTOR_STATE_ACTIVE);
        if (identityConfig.getReloadableConfig().getFeatureMultifactorLockingEnabled()) {
            //local locking is used, so reset the counters
            user.setMultiFactorFailedAttemptCount(null);
            user.setMultiFactorLastFailedTimestamp(null);
        }
    }

    private boolean handleMultiFactorEnable(User user, MultiFactor multiFactor, int verifiedOTPDevicesCount) {
        // Only mess with enabling/disabling mfa on user if non-null or changing factor type
        if (multiFactor.isEnabled() != null && multiFactor.isEnabled().booleanValue() != user.isMultiFactorEnabled()) {
            if (!userHasMultiFactorDevices(user)) {
                throw new IllegalStateException(ERROR_MSG_NO_DEVICE);
            } else if (!userHasVerifiedMultiFactorDevices(user, verifiedOTPDevicesCount)) {
                throw new IllegalStateException(ERROR_MSG_NO_VERIFIED_DEVICE);
            }

            return enableMultiFactorForUser(user, verifiedOTPDevicesCount);
        }
        return false;
    }

    private void handleMultiFactorUserEnforcementLevel(User user, MultiFactor multiFactor) {
        final UserMultiFactorEnforcementLevelEnum existingLevel = userMultiFactorEnforcementLevelConverter.convertTo(user.getUserMultiFactorEnforcementLevel(), null);

        if (multiFactor.getUserMultiFactorEnforcementLevel() != null
                && multiFactor.getUserMultiFactorEnforcementLevel() != existingLevel) {
            user.setUserMultiFactorEnforcementLevel(userMultiFactorEnforcementLevelConverter.convertFrom(multiFactor.getUserMultiFactorEnforcementLevel()));
            userService.updateUserForMultiFactor(user);
        }
    }

    private boolean handleMultiFactorType(User user, MultiFactor multiFactor, int verifiedOTPDevicesCount) {
        final boolean hasPhone = userHasVerifiedMobilePhone(user);
        final boolean hasOTPDevice = verifiedOTPDevicesCount > 0;
        final FactorTypeEnum factorType = multiFactor.getFactorType();
        boolean changed = false;
        if (factorType == null) {
            // Default values
            if (hasOTPDevice && !hasPhone) {
                user.setMultiFactorType(FactorTypeEnum.OTP.value());
                changed = true;
            } else if (!hasOTPDevice && hasPhone) {
                user.setMultiFactorType(FactorTypeEnum.SMS.value());
                changed = true;
            } else if (hasOTPDevice && hasPhone) {
                throw new BadRequestException("Must specify multi-factor authentication type to enable multi-factor authentication.");
            }
        } else {
            // Explicit set
            final boolean isOTP = FactorTypeEnum.OTP.equals(factorType);
            final boolean isSMS = FactorTypeEnum.SMS.equals(factorType);
            if (isOTP && hasOTPDevice) {
                user.setMultiFactorType(FactorTypeEnum.OTP.value());
                changed = true;
            } else if (isSMS && hasPhone) {
                user.setMultiFactorType(FactorTypeEnum.SMS.value());
                changed = true;
            } else if (isOTP && !hasOTPDevice) {
                throw new BadRequestException("User doesn't have a verified OTP device.");
            } else if (isSMS && !hasPhone) {
                throw new BadRequestException("User doesn't have a verified phone.");
            } else {
                throw new BadRequestException("Cannot set factor type '" + factorType.value() + "' to this user.");
            }
        }
        return changed;
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
        user.setMultiFactorState(null);
        user.setMultiFactorType(null);
        userService.updateUserForMultiFactor(user);
        otpDeviceDao.deleteAllOTPDevicesFromParent(user);

        if (enabled) {
            atomHopperClient.asyncPost(user, AtomHopperConstants.MULTI_FACTOR);
            emailClient.asyncSendMultiFactorDisabledMessage(user);
        }

        if (StringUtils.hasText(phoneRsId)) {
            MobilePhone phone = mobilePhoneDao.getById(phoneRsId);
            if (phone != null) {
                unlinkPhoneFromUser(phone, user);
            }
        }

        //note - if this fails we will have a orphaned user account in duo that is not linked to anything in ldap since
        //the info in ldap has been removed.
        if (StringUtils.hasText(providerUserId)) {
            deleteExternalUser(user.getId(), user.getUsername(), providerUserId);
        }
    }

    @Override
    public void updateMultiFactorDomainSettings(String domainId, MultiFactorDomain multiFactorDomain) {
        // Retrieve domain
        final Domain domain = domainService.checkAndGetDomain(domainId);

        // Update domain
        final String previousState = domain.getDomainMultiFactorEnforcementLevel();
        final DomainMultiFactorEnforcementLevelEnum newState = multiFactorDomain.getDomainMultiFactorEnforcementLevel();

        if (!newState.value().equalsIgnoreCase(previousState)) {
            domain.setDomainMultiFactorEnforcementLevel(newState.value());
            domainService.updateDomain(domain);

            // Revoke existing tokens when activated
            if (newState == DomainMultiFactorEnforcementLevelEnum.REQUIRED) {
                revokeAllMFAProtectedTokensByDomainId(domain);
            }
        }
    }

    /**
     * Revokes all tokens issued for MFA protected credentials for users within the specified domain that
     * <ol>
     *     <li>Have access to MFA</li>
     *     <li>MUST use MFA to login</li>
     *     <li><Do not currently have MFA enabled./li>
     * </ol>
     *
     * @param domain
     */
    private void revokeAllMFAProtectedTokensByDomainId(Domain domain) {
        for(User user : identityUserService.getProvisionedUsersByDomainId(domain.getDomainId())) {
            //only remove if user has access to MFA AND (does NOT have mfa enabled or is not required to use it)
            if (!(user.isMultiFactorEnabled()
                    || GlobalConstants.USER_MULTI_FACTOR_ENFORCEMENT_LEVEL_OPTIONAL.equals(user.getUserMultiFactorEnforcementLevelIfNullWillReturnDefault())
                    ||  (GlobalConstants.USER_MULTI_FACTOR_ENFORCEMENT_LEVEL_DEFAULT.equals(user.getUserMultiFactorEnforcementLevelIfNullWillReturnDefault())
                    && GlobalConstants.DOMAIN_MULTI_FACTOR_ENFORCEMENT_LEVEL_OPTIONAL.equals(domain.getDomainMultiFactorEnforcementLevelIfNullWillReturnOptional()))
            ) && isMultiFactorEnabledForUser(user)) {
                revokeAllMFAProtectedTokensForUser(user);
            }
        }
    }

    /**
     * A utility method to add a phone to a user. This will attempt to first create the mobile phone in ldap.
     * If the phone already exists it will then try to retrieve the phone from ldap and link the user to that phone.
     * If all attempts to link the user to the requested phone number, an exception will be thrown.
     *
     */
    private MobilePhone linkPhoneToUser(Phonenumber.PhoneNumber phoneNumber, User user) {
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
        user.setMultiFactorDeviceVerified(null);
        user.setMultiFactorDevicePin(null);
        user.setMultiFactorDevicePinExpiration(null);
        userService.updateUserForMultiFactor(user);

        return mobilePhone;
    }

    /**
     * A utility method to remove a user from a mobile phone. This method will check to see if the mobile phone is now orphaned
     * and delete the phone from persistent storage and the associated duo profile (if applicable). It will also remove
     * the phone from  If any errors occur while trying to delete the phone from duo or the directory, the error
     * will be caught and logged.
     *
     * Note
     *
     */
    private void unlinkPhoneFromUser(MobilePhone phone, User user) {
        Assert.notNull(phone);
        Assert.notNull(user);

        try {
            if (isPhoneUserMembershipEnabled()) {
                //get and unlink phone
                phone.removeMember(user);

                //delete the phone from the directory and external provider is no other links to phone exist
                if(CollectionUtils.isEmpty(phone.getMembers())) {
                    mobilePhoneDao.deleteObject(phone);
                    //only delete the duo phone if one exists && feature flagged on
                    if (identityConfig.getReloadableConfig().getFeatureDeleteUnusedDuoPhones() && org.apache.commons.lang.StringUtils.isNotBlank(phone.getExternalMultiFactorPhoneId())) {
                        try {
                            userManagement.deleteMobilePhone(phone.getExternalMultiFactorPhoneId());
                        } catch (Exception e) {
                            multiFactorConsistencyLogger.error(String.format("Error deleting phone '%s' from external provider. " +
                                    "The phone has been removed from the directory but still exists in the external provider.", phone.getExternalMultiFactorPhoneId()), e);
                        }
                    }
                } else {
                    mobilePhoneDao.updateObjectAsIs(phone);
                }
            }
        } catch (Exception e) {
            //eat the exception, but log it. This is a consistency issue where the phone will think some people are using it that are not, but will not cause an error in operation.
            multiFactorConsistencyLogger.error(String.format("Error removing user '%s' from phone '%s' membership. The phone membership will" +
                    "be inconsistent unless this is corrected. The user's DN should be removed from the phone's 'member' attribute.", user.getId(), phone.getId()), e);
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

        // First check if the user is locally locked (and fail fast)
        if (isUserLocalLocked(user)) {
            return new GenericMfaAuthenticationResponse(
                    MfaAuthenticationDecision.DENY,
                    MfaAuthenticationDecisionReason.LOCKEDOUT,
                    "Account is locked.",
                    null);
        }

        MfaAuthenticationResponse response = null;

        // Checks against OTP devices
        if (isMultiFactorTypeOTP(user)) {
            final Iterable<OTPDevice> otpDevices = otpDeviceDao.getVerifiedOTPDevicesByParent(user);
            for (Iterator<OTPDevice> it = otpDevices.iterator(); it.hasNext() && response == null;) {
                final OTPDevice otpDevice = it.next();
                if (otpHelper.checkTOTP(otpDevice.getKey(), passcode)) {
                    response = new GenericMfaAuthenticationResponse(
                            MfaAuthenticationDecision.ALLOW,
                            MfaAuthenticationDecisionReason.ALLOW,
                            "Authenticated by OTP device(" + otpDevice.getName() + ")",
                            null);
                }
            }
        }

        // Checks against mobile phones (Duo)
        if (response == null && isMultiFactorTypePhone(user)) {
            final MobilePhone phone = mobilePhoneDao.getById(user.getMultiFactorMobilePhoneRsId());
            verifyMultiFactorStateOnPhone(user, phone);
            response =  multiFactorAuthenticationService.verifyPasscodeChallenge(user.getExternalMultiFactorUserId(), phone.getExternalMultiFactorPhoneId(), passcode);

            // Check if it was locked on Duo, unlock and try again once [CIDMDEV-5058]
            if (response != null
                    && identityConfig.getReloadableConfig().getFeatureMultifactorLockingEnabled()
                    && MfaAuthenticationDecision.DENY.equals(response.getDecision())
                    && MfaAuthenticationDecisionReason.LOCKEDOUT.equals(response.getDecisionReason())
                    && response.getProviderResponse() != null) {
                userManagement.unlockUser(user.getExternalMultiFactorUserId());
                response =  multiFactorAuthenticationService.verifyPasscodeChallenge(user.getExternalMultiFactorUserId(), phone.getExternalMultiFactorPhoneId(), passcode);
            }
        }

        // Check against bypass codes
        if (response == null || MfaAuthenticationDecision.DENY.equals(response.getDecision())) {
            final boolean check = consumeBypassCodeForUser(user, passcode);
            if (check) {
                response = new GenericMfaAuthenticationResponse(
                        MfaAuthenticationDecision.ALLOW,
                        MfaAuthenticationDecisionReason.BYPASS,
                        "Authenticated by bypass code",
                        null);
            }
        }

        // If none worked and local locking is enabled, count one fail
        if ((response == null || MfaAuthenticationDecision.DENY.equals(response.getDecision())) &&
                incrementAndCheckLocalLockingOnUser(user)) {
            response = new GenericMfaAuthenticationResponse(
                    MfaAuthenticationDecision.DENY,
                    MfaAuthenticationDecisionReason.LOCKEDOUT,
                    "Incorrect passcode. Account is locked.",
                    null);
        }

        // If none worked and the response was not pre-populated by anything
        if (response == null) {
            response = new GenericMfaAuthenticationResponse(
                    MfaAuthenticationDecision.DENY,
                    MfaAuthenticationDecisionReason.DENY,
                    "Incorrect passcode. Please try again.",
                    null);
        }

        // If it successfully auth, reset the local locking counter
        if (response != null && MfaAuthenticationDecision.ALLOW.equals(response.getDecision())) {
            resetLocalLockingOnUser(user);
        }

        return response;
    }

    private boolean consumeBypassCodeForUser(User user, String passcode) {
        if (org.apache.commons.lang.StringUtils.isBlank(passcode)) {
            return false;
        }

        for (BypassDevice bypassDevice : getUnexpiredAndCleanExpiredBypassDevices(user)) {
            //encode the user provided code to compare to device codes
            String encodedCode = bypassHelper.encodeCodeForDevice(bypassDevice, passcode);
            for (String deviceEncodedCode : bypassDevice.getBypassCodes()) {
                if (encodedCode.equals(deviceEncodedCode)) {
                    consumeBypassCodeFromBypassDevice(bypassDevice, deviceEncodedCode);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * If it's the last bypass code for the device, delete the bypass code device; otherwise just remove the bypass
     * code.
     *
     * @param bypassDevice
     * @param bypassCode
     */
    private boolean consumeBypassCodeFromBypassDevice(BypassDevice bypassDevice, String bypassCode) {
        boolean removed = bypassDevice.getBypassCodes().remove(bypassCode);
        if (removed) {
            if (bypassDevice.getBypassCodes().size() == 0) {
                bypassDeviceDao.deleteBypassDevice(bypassDevice);
            } else {
                bypassDeviceDao.updateBypassDevice(bypassDevice);
            }
        }
        return removed;
    }

    private Iterable<BypassDevice> getUnexpiredAndCleanExpiredBypassDevices(UniqueId parent) {
        final List<BypassDevice> result = new ArrayList<BypassDevice>();
        for (BypassDevice bypassDevice : bypassDeviceDao.getAllBypassDevices(parent)) {
            if (bypassDevice.getMultiFactorDevicePinExpiration() == null ||
                    bypassDevice.getMultiFactorDevicePinExpiration().compareTo(new Date()) >= 0) {
                result.add(bypassDevice);
            } else {
                LOG.info("Clean expired bypass code: " + bypassDevice.getId());
                bypassDeviceDao.deleteBypassDevice(bypassDevice);
            }
        }
        return result;
    }

    /**
     * Increment the failed number of attempts on the user by 1 if the user is not already locked. Return whether or not
     * the user is locally locked (after taking into consideration the newly incremented number of failed attempts by 1).
     *
     * @param user
     * @return
     */
    private boolean incrementAndCheckLocalLockingOnUser(User user) {
        if (identityConfig.getReloadableConfig().getFeatureMultifactorLockingEnabled()) {
            if (isUserLocalLocked(user)) {
                return true;
            } else {
                //if the last failed timestamp has expired, we start over with 1
                if (user.getMultiFactorFailedAttemptCount() == null ||
                        hasUserMfaLastFailedTimeStampExpired(user)) {
                    user.setMultiFactorFailedAttemptCount(1);
                } else {
                    user.setMultiFactorFailedAttemptCount(user.getMultiFactorFailedAttemptCount() + 1);
                }
                user.setMultiFactorLastFailedTimestamp(new Date());
                userService.updateUserForMultiFactor(user);

                // Check if the user got locked
                return isUserLocalLocked(user);
            }
        }
        return false;
    }

    private void resetLocalLockingOnUser(User user) {
        if (identityConfig.getReloadableConfig().getFeatureMultifactorLockingEnabled() &&
                (user.getMultiFactorFailedAttemptCount() != null || user.getMultiFactorLastFailedTimestamp() != null)) {
            setMultiFactorUnlockState(user);
            userService.updateUserForMultiFactor(user);
        }
    }

    /**
     * A user is considered locked via local locking if the user has reached the maximum number of attempts AND the last
     * failure attempt was not longer than the login failure TTL.
     *
     * @param user
     * @return
     */
    @Override
    public boolean isUserLocalLocked(User user) {
        if (identityConfig.getReloadableConfig().getFeatureMultifactorLockingEnabled() &&
            !hasUserMfaLastFailedTimeStampExpired(user)) {
            return isUserReachedMaximumAttempts(user);
        }
        return false;
    }

    private boolean isUserReachedMaximumAttempts(User user) {
        final int max = identityConfig.getReloadableConfig().getFeatureMultifactorLockingMax();
        return user.getMultiFactorFailedAttemptCount() != null && user.getMultiFactorFailedAttemptCount() >= max;
    }

    /**
     * The last failed timestamp on the user is considered "expired" after a configurable time period has passed.
     *
     * @param user
     * @return
     */
    private boolean hasUserMfaLastFailedTimeStampExpired(User user) {
        final int expiration = identityConfig.getReloadableConfig().getFeatureMultifactorLoginFailureTtl();
        return new DateTime(user.getMultiFactorLastFailedTimestamp()).plusSeconds(expiration).isBeforeNow();
    }

    @Override
    public MultiFactorStateEnum getLogicalUserMultiFactorState(User user) {
        MultiFactorStateEnum logicalState = null;

        if (!user.isMultiFactorEnabled()) {
            //if user isn't using MFA, state is always null regardless of setting on user
            logicalState = null;
        } else if (!identityConfig.getReloadableConfig().getFeatureMultifactorLockingEnabled()) {
            //if using duo locking, just return whatever is on user. If user state is null, default to ACTIVE
            logicalState = multiFactorStateConverter.convertTo(user.getMultiFactorState(), null);
            if (logicalState == null) {
                logicalState = MultiFactorStateEnum.ACTIVE;
            }
        } else {
            /*
            if local locking is used, dynamically determine locking
             */
            if (isUserLocalLocked(user)) {
                logicalState = MultiFactorStateEnum.LOCKED;
            } else {
                logicalState = MultiFactorStateEnum.ACTIVE;
            }
        }

        return logicalState;
    }

    private void verifyMultiFactorStateOnUser(User user) {
        if (!user.isMultiFactorEnabled() || !userHasEnabledMultiFactorDevices(user)) {
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

    @Override
    public String getBypassCode(User user, Integer validSecs) {
        return getBypassCodes(user, validSecs, BigInteger.ONE, false).get(0);
    }

    @Override
    public List<String> getSelfServiceBypassCodes(User user, Integer validSecs, BigInteger numberOfCodes) {
        return getBypassCodes(user, validSecs, numberOfCodes, true);
    }

    private List<String> getBypassCodes(User user, Integer validSecs, BigInteger numberOfCodes, boolean isSelfService) {
        final int normalizedNumberOfCodes = getNumberOfCodes(numberOfCodes, isSelfService);
        final int normalizedValidSecs = isSelfService && validSecs == null ? 0 : validSecs;
        if (identityConfig.getReloadableConfig().getFeatureLocalMultifactorBypassEnabled()) {
            return createBypassCodes(user, normalizedNumberOfCodes, normalizedValidSecs);
        } else if (isMultiFactorTypeOTP(user)) {
            throw new BadRequestException("Bypass codes not currently supported for OTP devices");
        } else {
            return Arrays.asList(userManagement.getBypassCodes(user.getExternalMultiFactorUserId(), normalizedNumberOfCodes, normalizedValidSecs));
        }
    }

    private List<String> createBypassCodes(User user, int normalizedNumberOfCodes, int normalizedValidSecs) {
        bypassDeviceDao.deleteAllBypassDevices(user);
        final BypassDeviceCreationResult result = bypassHelper.createBypassDevice(normalizedNumberOfCodes, normalizedValidSecs);

        //persist the hashed codes to ldap
        bypassDeviceDao.addBypassDevice(user, result.getDevice());

        //return the plaintext codes
        return new ArrayList<String>(result.getPlainTextBypassCodes());
    }

    private int getNumberOfCodes(BigInteger requested, boolean isSelfService) {
        if (!isSelfService) {
            return identityConfig.getStaticConfig().getBypassDefaultNumber().max(BigInteger.ONE).intValue();
        }
        final BigInteger max = identityConfig.getStaticConfig().getBypassMaximumNumber();
        if (requested == null) {
            return max.intValue();
        } else {
            return max.min(requested).max(BigInteger.ONE).intValue();
        }
    }

    private boolean enableMultiFactorForUser(User user, int verifiedOTPDevicesCount) {
        if (verifiedOTPDevicesCount > 0 && isMultiFactorTypeOTP(user)) {
            return enableMultifactorAndPostFeed(user);
        } else if (isMultiFactorTypePhone(user)) {
            return enableMultiFactorUsingPhoneForUser(user);
        }
        return false;
    }

    private boolean enableMultiFactorUsingPhoneForUser(User user) {
        final MobilePhone phone = mobilePhoneDao.getById(user.getMultiFactorMobilePhoneRsId());

        final DuoPhone duoPhone = new DuoPhone();
        duoPhone.setNumber(phone.getTelephoneNumber());

        final DuoUser duoUser = new DuoUser();
        duoUser.setUsername(user.getId());

        final ProviderUser providerUser = userManagement.createUser(duoUser);
        user.setExternalMultiFactorUserId(providerUser.getProviderId());

        try {
            ProviderPhone providerPhone = userManagement.linkMobilePhoneToUser(providerUser.getProviderId(), duoPhone);
            phone.setExternalMultiFactorPhoneId(providerPhone.getProviderId());
        } catch (com.rackspace.identity.multifactor.exceptions.NotFoundException e) {
            // Translate to IDM not found exception.
            // Thrown if user does not exist in duo, though it should since was created above
            multiFactorConsistencyLogger.error(String.format("An error occurred enabling multifactor for user '%s'. Duo returned a NotFoundException for the user's duo profile '%s'.", user.getId(), providerUser.getProviderId()), e);
            throw new NotFoundException(e.getMessage(), e);
        }

        mobilePhoneDao.updateObjectAsIs(phone);
        return enableMultifactorAndPostFeed(user);
    }

    private boolean enableMultifactorAndPostFeed(User user) {
        final boolean alreadyEnabled = user.isMultiFactorEnabled();

        user.setMultifactorEnabled(true);
        user.setMultiFactorState(MULTI_FACTOR_STATE_ACTIVE);

        if (!alreadyEnabled) {
            revokeAllMFAProtectedTokensForUser(user);
            atomHopperClient.asyncPost(user, AtomHopperConstants.MULTI_FACTOR);
            emailClient.asyncSendMultiFactorEnabledMessage(user);
        }

        return true;
    }

    /**
     * Revokes all the non-mfa tokens for the user, skipping those tokens issued by credentials that are NOT protected
     * via MFA.
     */
    private void revokeAllMFAProtectedTokensForUser(User user) {
        //only revoke password tokens
        tokenRevocationService.revokeTokensForBaseUser(user, TokenRevocationService.AUTH_BY_LIST_PASSWORD_TOKENS);
    }

    private void disableMultiFactorForUser(User user) {
        String providerUserId = user.getExternalMultiFactorUserId();

        boolean enabled = user.isMultiFactorEnabled();

        user.setMultifactorEnabled(false);
        user.setMultiFactorState(null);
        user.setExternalMultiFactorUserId(null);
        user.setMultiFactorType(null);
        user.setMultiFactorFailedAttemptCount(null);
        user.setMultiFactorLastFailedTimestamp(null);
        userService.updateUserForMultiFactor(user);

        if (enabled){
            atomHopperClient.asyncPost(user, AtomHopperConstants.MULTI_FACTOR);
            emailClient.asyncSendMultiFactorDisabledMessage(user);
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

    @Override
    public boolean isMultiFactorGloballyEnabled() {
        return isMultiFactorEnabled() && !isMultiFactorBetaEnabled();
    }

    @Override
    public boolean isMultiFactorEnabled() {
        return identityConfig.getStaticConfig().getMultiFactorServicesEnabled();
    }

    @Override
    public boolean isMultiFactorEnabledForUser(BaseUser user) {
        if(!isMultiFactorEnabled()) {
            return false;
        } else if(isMultiFactorBetaEnabled()) {
            return userHasMultiFactorBetaRole(user);
        } else {
            return true;
        }
    }

    @Override
    public OTPDevice addOTPDeviceToUser(String userId, String name) {
        Assert.notNull(name);
        Assert.notNull(userId);

        final User user = userService.checkAndGetUserById(userId);

        List<OTPDevice> userDevices = getOTPDevicesForUser(user);

        if (userDevices.size() >= identityConfig.getReloadableConfig().getMaxOTPDevicesPerUser()) {
            throw new BadRequestException(MAX_OTP_DEVICES_REACHED);
        }

        final OTPDevice device = otpHelper.createOTPDevice(name);
        otpDeviceDao.addOTPDevice(user, device);
        return device;
    }

    @Override
    public OTPDevice checkAndGetOTPDeviceFromUserById(String userId, String deviceId) {
        Assert.notNull(userId);
        Assert.notNull(deviceId);
        final User user = userService.checkAndGetUserById(userId);
        final OTPDevice device =  getOTPDeviceFromUserById(user, deviceId);
        if (device == null) {
            throw new NotFoundException("OTP device not found");
        }
        return device;
    }

    @Override
    public OTPDevice getOTPDeviceFromUserById(String userId, String deviceId) {
        Assert.notNull(userId);
        Assert.notNull(deviceId);
        final User user = userService.getUserById(userId);
        final OTPDevice device = getOTPDeviceFromUserById(user, deviceId);
        return device;
    }

    @Override
    public List<OTPDevice> getOTPDevicesForUser(User user) {
        Assert.notNull(user);
        Assert.notNull(user.getUniqueId(), "User must have been retrieved from backend and have a populated DN");
        Iterable<OTPDevice> devicesAsIterable = otpDeviceDao.getOTPDevicesByParent(user);
        List<OTPDevice> devices = IteratorUtils.toList(devicesAsIterable.iterator());

        return devices;
    }

    public OTPDevice getOTPDeviceFromUserById(User user, String deviceId) {
        if (user == null) {
            return null;
        }
        final OTPDevice device = otpDeviceDao.getOTPDeviceByParentAndId(user, deviceId);
        return device;
    }

    @Override
    public void verifyOTPDeviceForUserById(String userId, String deviceId, String verificationCode) {
        Assert.notNull(verificationCode);

        final OTPDevice device = checkAndGetOTPDeviceFromUserById(userId, deviceId);
        if (Boolean.TRUE.equals(device.getMultiFactorDeviceVerified())) {
            throw new MultiFactorDeviceAlreadyVerifiedException(DEVICE_ALREADY_VERIFIED);
        } else if (otpHelper.checkTOTP(device.getKey(), verificationCode)) {
            device.setMultiFactorDeviceVerified(true);
            otpDeviceDao.updateObject(device);
        } else {
            throw new MultiFactorDevicePinValidationException(PIN_DOES_NOT_MATCH);
        }
    }

    public void deleteOTPDeviceForUser(String userId, String deviceId) {
        final User user = userService.checkAndGetUserById(userId);
        final OTPDevice device = getOTPDeviceFromUserById(user, deviceId);

        if (device != null) {
            FactorTypeEnum userMfaType = user.getMultiFactorTypeAsEnum();

            //check the conditions that would allow the device to be deleted, in order of likelihood and expense to check
            if (!device.getMultiFactorDeviceVerified() || !user.isMultiFactorEnabled()
                    || userMfaType == null || userMfaType == FactorTypeEnum.SMS
                    || otpDeviceDao.countVerifiedOTPDevicesByParent(user) > 1) {
                //allowed to be deleted
                otpDeviceDao.deleteObject(device);
            } else {
                throw new ErrorCodeIdmException(ErrorCodes.ERROR_CODE_DELETE_OTP_DEVICE_FORBIDDEN_STATE, DELETE_OTP_DEVICE_REQUEST_INVALID_MSG);
            }
        } else {
            throw new NotFoundException("The device was not found on the specified user");
        }
    }

    @Override
    public List<MultiFactorDevice> getMultiFactorDevicesForUser(User user) {
        Assert.notNull(user);
        Assert.notNull(user.getUniqueId(), "User must have been retrieved from backend and have a populated DN");

        List<MultiFactorDevice> mfaDevices = new ArrayList<MultiFactorDevice>();

        List<MobilePhone> phones = getMobilePhonesForUser(user);
        List<OTPDevice> otpDevices = getOTPDevicesForUser(user);

        org.apache.commons.collections4.CollectionUtils.addAll(mfaDevices, phones);
        org.apache.commons.collections4.CollectionUtils.addAll(mfaDevices, otpDevices);

        return mfaDevices;
    }

    @Override
    public boolean userHasMultiFactorDevices(BaseUser user) {
        return user instanceof User && userHasMultiFactorDevices((User) user, otpDeviceDao.countOTPDevicesByParent(user));
    }

    @Override
    public boolean userHasVerifiedMultiFactorDevices(BaseUser user) {
        return user instanceof User && userHasVerifiedMultiFactorDevices((User) user, otpDeviceDao.countVerifiedOTPDevicesByParent(user));
    }

    @Override
    public String getMultiFactorType(BaseUser baseUser) {
        if (baseUser instanceof User) {
            final User user = (User) baseUser;
            if (user.getMultiFactorType() != null) {
                return user.getMultiFactorType();
            } else if (userHasEnabledMobilePhone(user)) {
                return FactorTypeEnum.SMS.value();
            }
        }
        return null;
    }

    @Override
    public boolean isMultiFactorTypePhone(BaseUser user) {
        return FactorTypeEnum.SMS.value().equals(getMultiFactorType(user));
    }

    @Override
    public boolean isMultiFactorTypeOTP(BaseUser user) {
        return FactorTypeEnum.OTP.value().equals(getMultiFactorType(user));
    }

    private boolean userHasMultiFactorDevices(User user, int verifiedOTPDevicesCount) {
        boolean hasOTPDevice = verifiedOTPDevicesCount > 0;
        boolean hasPhone = StringUtils.hasText(user.getMultiFactorMobilePhoneRsId());
        return hasOTPDevice || hasPhone;
    }

    private boolean userHasVerifiedMultiFactorDevices(User user, int verifiedOTPDevicesCount) {
        boolean hasOTPDevice = verifiedOTPDevicesCount > 0;
        boolean hasVerifiedPhone = userHasVerifiedMobilePhone(user);
        return hasOTPDevice || hasVerifiedPhone;
    }

    private boolean userHasEnabledMultiFactorDevices(User user) {
        return userHasEnabledMultiFactorDevices(user, otpDeviceDao.countVerifiedOTPDevicesByParent(user));
    }
    private boolean userHasEnabledMultiFactorDevices(User user, int verifiedOTPDevicesCount) {
        boolean hasOTPDevice = verifiedOTPDevicesCount > 0;
        boolean hasEnabledPhone = userHasEnabledMobilePhone(user);
        return user.isMultiFactorEnabled() && (hasOTPDevice || hasEnabledPhone);
    }

    private boolean userHasEnabledMobilePhone(User user) {
        return userHasVerifiedMobilePhone(user) && StringUtils.hasText(user.getExternalMultiFactorUserId());
    }

    private boolean userHasVerifiedMobilePhone(User user) {
        return StringUtils.hasText(user.getMultiFactorMobilePhoneRsId()) && user.isMultiFactorDeviceVerified();
    }

    private boolean isMultiFactorBetaEnabled() {
        return identityConfig.getStaticConfig().getMultiFactorBetaEnabled();
    }

    private boolean userHasMultiFactorBetaRole(BaseUser user) {
        List<TenantRole> userGlobalRoles = tenantService.getGlobalRolesForUser(user);

        if(userGlobalRoles != null && !userGlobalRoles.isEmpty()) {
            for(TenantRole role : userGlobalRoles) {
                if(role.getName().equals(identityConfig.getStaticConfig().getMultiFactorBetaRoleName())) {
                    return true;
                }
            }
        }
        return false;
    }

}
