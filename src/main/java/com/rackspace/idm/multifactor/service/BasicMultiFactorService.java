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
    public static final String ERROR_MSG_NO_VERIFIED_PHONE = "User doesn't have a verified phone.";
    public static final String ERROR_MSG_NO_VERIFIED_OTP_DEVICE = "User doesn't have a verified OTP device.";

    private static final String EXTERNAL_PROVIDER_ERROR_FORMAT = "operation={},userId={},username={},externalId={},externalResponse={}";
    public static final String DELETE_OTP_DEVICE_REQUEST_INVALID_MSG = "You can not delete the last verified OTP device when your account is configured to use OTP multifactor.";

    public static final String DELETE_MOBILE_PHONE_REQUEST_INVALID_MSG = "You can not delete your mobile phone when your account is configured to use SMS multifactor.";

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
                return replacePhoneOnUserAndSaveUpdatedUser(user, phoneNumber);
            }
        } else {
            return linkPhoneToUserAndSaveUpdatedUser(phoneNumber, user);
        }
    }

    /**
     * Use this service to:
     *
     * 1. Setup a phone on a user and leave MFA inactive (phone + false)
     * 2. Setup a phone on a user and enable MFA (phone + true)
     *
     * The goal is to specify the requested END state
     * @param userId
     * @param phoneNumber
     */
    @Override
    public void setupSmsForUser(String userId, Phonenumber.PhoneNumber phoneNumber) {
        Assert.notNull(phoneNumber);
        Assert.notNull(userId);

        User user = userService.checkAndGetUserById(userId);

        if(user.isMultiFactorEnabled()) {
            throw new IllegalArgumentException("User has MFA enabled. Can not use this method.");
        }
        if(user.getMultiFactorMobilePhoneRsId() != null) {
            throw new IllegalArgumentException("User already has a phone. Can not use this method.");
        }

        MobilePhone phone = linkPhoneToUser(phoneNumber, user); //creates and saves the mobile phone, and sets link on user
        user.setMultiFactorDeviceVerified(true);
        user.setMultiFactorType(FactorTypeEnum.SMS.value()); //must set the factor type prior to enabling
        enableMultiFactorForUser(user, true, false); //this includes saving the user
    }

    /**
     * Replace the device on the user, resetting all verification of the device on the user, and saves the updated user.
     * If the phone is already linked to the user, this is a NO-OP and just returns the existing device.
     * @param user
     * @param phoneNumber
     */
    private MobilePhone replacePhoneOnUserAndSaveUpdatedUser(User user, Phonenumber.PhoneNumber phoneNumber) {
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
            newPhone = linkPhoneToUserAndSaveUpdatedUser(phoneNumber, user);

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

    private MobilePhone getMobilePhoneById(String mobilePhoneId) {
        return mobilePhoneDao.getById(mobilePhoneId);
    }

    @Override
    public MobilePhone checkAndGetMobilePhoneFromUser(User user, String mobilePhoneId) {
        MobilePhone mobilePhone = null;
        if (user != null
                && org.apache.commons.lang.StringUtils.isNotBlank(mobilePhoneId)) {
            //make sure the user is associated with this phone
            if (mobilePhoneId.equals(user.getMultiFactorMobilePhoneRsId())) {
                mobilePhone = getMobilePhoneById(mobilePhoneId);
            }
        }

        if (mobilePhone == null) {
            //use same error regardless of why phone not found (e.g. even if user was null)
            throw new NotFoundException("Mobile phone not found");
        }

        return mobilePhone;
    }

    public void deleteMobilePhoneFromUser(User user, String mobilePhoneId) {
        MobilePhone phone = checkAndGetMobilePhoneFromUser(user, mobilePhoneId);

        if (phone != null) {
            FactorTypeEnum userMfaType = user.getMultiFactorTypeAsEnum();

            //check the conditions that would allow the device to be deleted
            if (!user.isMultiFactorEnabled() || userMfaType == FactorTypeEnum.OTP) {
                //reload the user to guarantee state is correct on user updating.
                User userToUpdate = userService.checkAndGetUserById(user.getId());

                //null out phone specific MFA stuff
                userToUpdate.setMultiFactorMobilePhoneRsId(null);
                userToUpdate.setMultiFactorDeviceVerified(null);
                userToUpdate.setMultiFactorDevicePin(null);
                userToUpdate.setMultiFactorDevicePinExpiration(null);

                userService.updateUserForMultiFactor(userToUpdate);

                //Unlink the phone from the user
                unlinkPhoneFromUser(phone, user);
            } else {
                throw new ErrorCodeIdmException(ErrorCodes.ERROR_CODE_DELETE_MOBILE_PHONE_FORBIDDEN_STATE, DELETE_MOBILE_PHONE_REQUEST_INVALID_MSG);
            }
        } else {
            throw new NotFoundException("The phone was not found on the specified user");
        }
    }

    @Override
    public void updateMultiFactorSettings(String userId, MultiFactor multiFactor) {
        final User user = userService.checkAndGetUserById(userId);

        if (Boolean.TRUE.equals(multiFactor.isEnabled()) ||
                (multiFactor.getFactorType() != null)) {
            // Validate for enable multi-factor or change factor type
            if (multiFactor.isEnabled() == null
                    && multiFactor.getFactorType() != null
                    && Boolean.FALSE.equals(user.isMultiFactorEnabled())) {
                throw new BadRequestException("Can only set factor type when MFA is already enabled or while enabling MFA");
            } else if (multiFactor.isUnlock() != null
                    || multiFactor.getUserMultiFactorEnforcementLevel() != null) {
                throw new BadRequestException("Cannot change other settings while setting multi factor.");
            }
           handleMFAEnablementAndFactorType(user, multiFactor);

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

    /**
     * Supports 3 use cases:
     * <ol>
     * <li>User enabling MFA (when previously disabled) without specifying a factorType (in which case a default is chosen)</li>
     * <li>User enabling MFA (when previously disabled) along with specifying a factorType</li>
     * <li>User switching factor types with MFA already enabled</li>
     * </ol>
     *
     * <p>
     * Other scenarios:
     * <ul>
     *     <li>If the user already has MFA enabled and either don't specify a factor type or specify what they are already using,
     * this is a No-Op</li>
     * <li>If the user does not have MFA enabled and doesn't request it be enabled, throws an IllegalArgumentException</li>
     * <li>If the user specifies a factorType for which they have no verified devices, a BadRequestException will be throw</li>
     * <li>If the user is enabling MFA, but does not specify a factor type and either has no verified devices or verified
     * devices of multiple types (e.g. both a verified phone and OTP device) a BadRequestException will be thrown</li>
     * </ul>
     *
     * The end result of this method returning successfully (without an error) is that the user has MFA enabled with
     * either the specified factorType or a chosen default (whichever type has a verified device)
     * </p>
     *
     * <p>If the user is enabling MFA from a disabled state, will revoke all protected tokens,
     * send enablement email, and update user feed event.</p>
     *
     * @param user
     * @param multiFactor
     */
    private void handleMFAEnablementAndFactorType(User user, MultiFactor multiFactor) {
        boolean userAlreadyHasMFAEnabled = user.isMultiFactorEnabled();
        FactorTypeEnum currentEffectiveMFAType = getMultiFactorType(user);
        FactorTypeEnum requestedMFAType = multiFactor.getFactorType();
        boolean factorTypeChange = requestedMFAType != null && requestedMFAType != currentEffectiveMFAType;

        if (!userAlreadyHasMFAEnabled && !Boolean.TRUE.equals(multiFactor.isEnabled())) {
            throw new IllegalArgumentException("Must request for MFA to be enabled");
        } else if (userAlreadyHasMFAEnabled && !factorTypeChange) {
            return; //no op. MFA already enabled, factor type is already set appropriately (e.g user asked for SMS, but SMS already used)
        }

        FactorTypeEnum finalFactorType = null;
        if (requestedMFAType != null) {
            //User is specifying a factor type. Determine if allowed
            verifyUserCanEnableFactorType(user, requestedMFAType);
            finalFactorType = requestedMFAType;
        } else {
            //the user did not specify a factor type. Only way could reach this part of code is if they are enabling
            // mfa from a disabled state so need to determine if we can set a default. This throws error if user state
            //is not set up to allow a default to be chosen
            finalFactorType = calculateDefaultFactorTypeForUser(user);
        }

        String externalUserId = user.getExternalMultiFactorUserId();
        user.setMultiFactorType(finalFactorType.value());
        if (finalFactorType == FactorTypeEnum.OTP) {
            user.setExternalMultiFactorUserId(null); //OTP doesn't use this. Doesn't matter if was already null. just make sure it is now.
        } else if (finalFactorType == FactorTypeEnum.SMS) {
            //no clean up necessary moving to SMS from OTP
        } else {
            //will never happen - unless we expand to more factors and don't add support in.
            throw new IllegalStateException(String.format("Unrecognized factorType '%s'", finalFactorType));
        }

        /*
        enable MFA and save the user. if user did NOT already have MFA enabled (e.g. - not just switching MFA factor types), then revoke outstanding
        tokens and send feed events.
         */
        enableMultiFactorForUser(user, !userAlreadyHasMFAEnabled, !userAlreadyHasMFAEnabled);

        //at this point user is setup for new factor type. Duo is setup, etc.

        //If using OTP and a profile previously existed in DUO for this user, delete the profile
        if (finalFactorType == FactorTypeEnum.OTP && org.apache.commons.lang.StringUtils.isNotBlank(externalUserId)) {
            deleteExternalUser(user.getId(), user.getUsername(), externalUserId);
        }
    }

    private void handleMultiFactorUnlock(User user, MultiFactor multiFactor) {
        if (Boolean.TRUE.equals(multiFactor.isUnlock())) {
            //want to try and unlock user regardless of mfa enable/disable
            setMultiFactorUnlockState(user);
            userService.updateUserForMultiFactor(user);
        }
    }

    private void setMultiFactorUnlockState(User user) {
        user.setMultiFactorFailedAttemptCount(null);
        user.setMultiFactorLastFailedTimestamp(null);
    }

    private void handleMultiFactorUserEnforcementLevel(User user, MultiFactor multiFactor) {
        final UserMultiFactorEnforcementLevelEnum existingLevel = userMultiFactorEnforcementLevelConverter.convertTo(user.getUserMultiFactorEnforcementLevel(), null);

        if (multiFactor.getUserMultiFactorEnforcementLevel() != null
                && multiFactor.getUserMultiFactorEnforcementLevel() != existingLevel) {
            user.setUserMultiFactorEnforcementLevel(userMultiFactorEnforcementLevelConverter.convertFrom(multiFactor.getUserMultiFactorEnforcementLevel()));
            userService.updateUserForMultiFactor(user);
        }
    }

    /**
     * Throws a BadRequestException if the user's state will not support the user enabling the specified type of MFA
     * @param user
     * @param factorTypeEnum
     */
    private void verifyUserCanEnableFactorType(User user, FactorTypeEnum factorTypeEnum) {
        if (factorTypeEnum == FactorTypeEnum.OTP) {
            final int verifiedOTPDevicesCount = otpDeviceDao.countVerifiedOTPDevicesByParent(user);
            if (verifiedOTPDevicesCount == 0) {
                throw new BadRequestException(ERROR_MSG_NO_VERIFIED_OTP_DEVICE);
            }
        } else if (factorTypeEnum == FactorTypeEnum.SMS) {
            if (!userHasVerifiedMobilePhone(user)) {
                throw new BadRequestException(ERROR_MSG_NO_VERIFIED_PHONE);
            }
        } else {
            throw new BadRequestException("Cannot set factor type '" + factorTypeEnum.value() + "' on this user.");
        }
    }

    private FactorTypeEnum calculateDefaultFactorTypeForUser(User user) {
        final boolean hasVerifiedPhone = userHasVerifiedMobilePhone(user);
        final boolean hasVerifiedOTPDevice = otpDeviceDao.countVerifiedOTPDevicesByParent(user) > 0;
        FactorTypeEnum defaultVal = null;

        // Default values
        if (hasVerifiedOTPDevice && !hasVerifiedPhone) {
            defaultVal = FactorTypeEnum.OTP;
        } else if (hasVerifiedPhone && !hasVerifiedOTPDevice) {
            defaultVal = FactorTypeEnum.SMS;
        } else if (hasVerifiedOTPDevice && hasVerifiedPhone) {
            throw new BadRequestException("Must specify multi-factor authentication type to enable multi-factor authentication.");
        } else {
            //user doesn't have either a verified phone or verified OTP device
            if (!userHasMultiFactorDevices(user)) {
                throw new BadRequestException(ERROR_MSG_NO_DEVICE);
            } else {
                throw new BadRequestException(ERROR_MSG_NO_VERIFIED_DEVICE);
            }
        }
        return defaultVal;
    }

    @Override
    public void removeMultiFactorForUser(String userId) {
        User user = userService.checkAndGetUserById(userId);
        boolean enabled = user.isMultiFactorEnabled();

        removeMultifactorFromUserWithoutNotifications(user);

        if (enabled) {
            atomHopperClient.asyncPost(user, AtomHopperConstants.MULTI_FACTOR);
            emailClient.asyncSendMultiFactorDisabledMessage(user);
        }
    }

    @Override
    public void removeMultifactorFromUserWithoutNotifications(User user) {
        String providerUserId = user.getExternalMultiFactorUserId();
        String phoneRsId = user.getMultiFactorMobilePhoneRsId();

        //reset user
        user.setMultifactorEnabled(null);
        user.setExternalMultiFactorUserId(null);
        user.setMultiFactorMobilePhoneRsId(null);
        user.setMultiFactorDevicePin(null);
        user.setMultiFactorDeviceVerified(null);
        user.setMultiFactorDevicePinExpiration(null);
        user.setMultiFactorType(null);
        userService.updateUserForMultiFactor(user);
        otpDeviceDao.deleteAllOTPDevicesFromParent(user);

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
            )) {
                revokeAllMFAProtectedTokensForUser(user);
            }
        }
    }

    /**
     * A utility method to add a phone to a user. This will attempt to first create the mobile phone in ldap.
     * If the phone already exists it will then try to retrieve the phone from ldap and link the user to that phone.
     * If all attempts to link the user to the requested phone number, an exception will be thrown.
     *
     * The user is updated, but NOT saved. The caller MUST save the updated user.
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

        return mobilePhone;
    }

    /**
     * Does everything that {@link #linkPhoneToUser(Phonenumber.PhoneNumber, User)} does, and saves user at end
     * @param phoneNumber
     * @param user
     * @return
     */
    private MobilePhone linkPhoneToUserAndSaveUpdatedUser(Phonenumber.PhoneNumber phoneNumber, User user) {
        MobilePhone mobilePhone = linkPhoneToUser(phoneNumber, user);
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
                    mobilePhoneDao.deleteMobilePhone(phone);
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
                    mobilePhoneDao.updateMobilePhone(phone);
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

    /**
     * Saves the user as necessary. For example, when a valid passcode is provided, the invalid counters on the user are
     * reset and the user is saved. When an invalid passcode is provided, the counters are incremented, and the user is saved.
     *
     * @param userId
     * @param passcode
     * @return
     */
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
                    && MfaAuthenticationDecision.DENY.equals(response.getDecision())
                    && MfaAuthenticationDecisionReason.LOCKEDOUT.equals(response.getDecisionReason())
                    && response.getProviderResponse() != null) {
                LOG.warn(String.format("User '%s' is locked in Duo. Will unlock user in Duo and try again.", userId));
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

        // If none worked, count one fail, and send locked message if account is locked (or becomes locked)
        if ((response == null || MfaAuthenticationDecision.DENY.equals(response.getDecision())) &&
                incrementAndCheckLocalLockingOnUser(user)) {
            response = new GenericMfaAuthenticationResponse(
                    MfaAuthenticationDecision.DENY,
                    MfaAuthenticationDecisionReason.LOCKEDOUT,
                    "Incorrect passcode. Account is locked.",
                    null);
        }

        // If none worked, but account is not locked after incrementing failure count, return general error.
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

    private void resetLocalLockingOnUser(User user) {
        if (user.getMultiFactorFailedAttemptCount() != null || user.getMultiFactorLastFailedTimestamp() != null) {
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
        if (!hasUserMfaLastFailedTimeStampExpired(user)) {
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
        } else {
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

    /**
     * Configures the user and MFA devices as necessary to enable MFA. The user is updated in backend. Assumes the user
     * has the multifactor type set appropriate already.
     *
     * @param user
     */
    private void enableMultiFactorForUser(User user, boolean revokeTokens, boolean sendNotifications) {
        //not a fan of boolean parameters controlling behavior. However, in this case wanted to minimize changes due
        //to temp service add. Such parameters on an internal (private) method are much easier to revert.
        user.setMultifactorEnabled(true);

        /*
        if setting up SMS, must configure Duo profiles (User and Phone) and add links to local entries.
        OTP doesn't require anything else to be done
         */
        if (isMultiFactorTypePhone(user)) {
            setupDuoSmsProfilesOnUser(user);
        }

        userService.updateUserForMultiFactor(user);

        if (revokeTokens) {
            revokeAllMFAProtectedTokensForUser(user);
        }
        if (sendNotifications) {
            atomHopperClient.asyncPost(user, AtomHopperConstants.MULTI_FACTOR);
            emailClient.asyncSendMultiFactorEnabledMessage(user);
        }
    }

    /**
     * Configures Duo for the user. Creates user and phone profiles within Duo if necessary. The local phone entry is
     * updated and saved if required. The user is updated, but NOT saved. The caller must save the user.
     *
     * @param user
     */
    private void setupDuoSmsProfilesOnUser(User user) {
        final MobilePhone phone = mobilePhoneDao.getById(user.getMultiFactorMobilePhoneRsId());

        final DuoPhone duoPhone = new DuoPhone();
        duoPhone.setNumber(phone.getTelephoneNumber());

        final DuoUser duoUser = new DuoUser();
        duoUser.setUsername(user.getId());

        final ProviderUser providerUser = userManagement.createUser(duoUser);
        user.setExternalMultiFactorUserId(providerUser.getProviderId());

        try {
            ProviderPhone providerPhone = userManagement.linkMobilePhoneToUser(providerUser.getProviderId(), duoPhone);
            if (!providerPhone.getProviderId().equals(phone.getExternalMultiFactorPhoneId())) {
                //a Duo phone was created for this device that differs from what's recorded. Must update local phone entry
                phone.setExternalMultiFactorPhoneId(providerPhone.getProviderId());
                mobilePhoneDao.updateMobilePhone(phone);
            }
        } catch (com.rackspace.identity.multifactor.exceptions.NotFoundException e) {
            // Translate to IDM not found exception.
            // Thrown if user does not exist in duo, though it should since was created above
            multiFactorConsistencyLogger.error(String.format("An error occurred enabling multifactor for user '%s'. Duo returned a NotFoundException for the user's duo profile '%s'.", user.getId(), providerUser.getProviderId()), e);
            throw new NotFoundException(e.getMessage(), e);
        }
    }

    /**
     * Revokes all the non-mfa tokens for the user, skipping those tokens issued by credentials that are NOT protected
     * via MFA.
     */
    private void revokeAllMFAProtectedTokensForUser(User user) {
        //only revoke password tokens
        tokenRevocationService.revokeTokensForEndUser(user, TokenRevocationService.AUTH_BY_LIST_REVOKE_ON_MFA_ENABLE);
    }

    private void disableMultiFactorForUser(User user) {
        String providerUserId = user.getExternalMultiFactorUserId();

        boolean enabled = user.isMultiFactorEnabled();

        user.setMultifactorEnabled(false);
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

        mobilePhoneDao.addMobilePhone(mobilePhone);
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
                mobilePhoneDao.updateMobilePhone(mobilePhone);
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
        return false;
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
            otpDeviceDao.updateOTPDevice(device);
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
                otpDeviceDao.deleteOTPDevice(device);
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
    public FactorTypeEnum getMultiFactorType(BaseUser baseUser) {
        if (baseUser instanceof User) {
            final User user = (User) baseUser;
            return user.getMultiFactorTypeAsEnum();
        }
        return null;
    }

    @Override
    public boolean isMultiFactorTypePhone(BaseUser user) {
        return FactorTypeEnum.SMS == getMultiFactorType(user);
    }

    @Override
    public boolean isMultiFactorTypeOTP(BaseUser user) {
        return FactorTypeEnum.OTP == getMultiFactorType(user);
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

}
