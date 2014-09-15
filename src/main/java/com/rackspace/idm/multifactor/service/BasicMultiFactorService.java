package com.rackspace.idm.multifactor.service;

import com.google.i18n.phonenumbers.Phonenumber;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.DomainMultiFactorEnforcementLevelEnum;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.MultiFactor;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserMultiFactorEnforcementLevelEnum;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.MultiFactorDomain;
import com.rackspace.identity.multifactor.domain.MfaAuthenticationResponse;
import com.rackspace.identity.multifactor.domain.Pin;
import com.rackspace.identity.multifactor.exceptions.MultiFactorException;
import com.rackspace.identity.multifactor.providers.*;
import com.rackspace.identity.multifactor.providers.duo.domain.DuoPhone;
import com.rackspace.identity.multifactor.providers.duo.domain.DuoUser;
import com.rackspace.identity.multifactor.util.IdmPhoneNumberUtil;
import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperClient;
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperConstants;
import com.rackspace.idm.api.resource.cloud.email.EmailClient;
import com.rackspace.idm.domain.dao.MobilePhoneDao;
import com.rackspace.idm.domain.dozer.converters.UserMultiFactorEnforcementLevelConverter;
import com.rackspace.idm.domain.entity.MobilePhone;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
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

    public static final String MULTIFACTOR_BETA_ROLE_NAME = "cloudAuth.multiFactorBetaRoleName";

    public static final String ERROR_MSG_SAVE_OR_UPDATE_USER = "Error updating user %s";
    public static final String ERROR_MSG_PHONE_NOT_ASSOCIATED_WITH_USER = "Specified phone is not associated with user";

    public static final String ERROR_MSG_NO_DEVICE = "User not associated with a multifactor device";
    public static final String ERROR_MSG_NO_VERIFIED_DEVICE = "Device not verified";

    private static final String EXTERNAL_PROVIDER_ERROR_FORMAT = "operation={},userId={},username={},externalId={},externalResponse={}";

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
    private final Logger multiFactorConsistencyLogger = LoggerFactory.getLogger(GlobalConstants.MULTIFACTOR_CONSISTENCY_LOG_NAME);

    public static final String CONFIG_PROP_PHONE_MEMBERSHIP_ENABLED = "feature.multifactor.phone.membership.enabled";

    public static final String BYPASS_DEFAULT_NUMBER = "multifactor.bypass.default.number";
    public static final String BYPASS_MAXIMUM_NUMBER = "multifactor.bypass.maximum.number";

    public static final String MULTI_FACTOR_STATE_ACTIVE = "ACTIVE";
    public static final String MULTI_FACTOR_STATE_LOCKED = "LOCKED";

    public static final List<List<String>> AUTHENTICATEDBY_LIST_TO_NOT_REVOKE_ON_MFA_ENABLEMENT = Arrays.asList(Arrays.asList(GlobalConstants.AUTHENTICATED_BY_APIKEY, GlobalConstants.AUTHENTICATED_BY_PASSCODE)
            , Arrays.asList(GlobalConstants.AUTHENTICATED_BY_PASSWORD, GlobalConstants.AUTHENTICATED_BY_PASSCODE)
            , Arrays.asList(GlobalConstants.AUTHENTICATED_BY_APIKEY));

    private final UserMultiFactorEnforcementLevelConverter userMultiFactorEnforcementLevelConverter = new UserMultiFactorEnforcementLevelConverter();

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

    @Autowired
    private EmailClient emailClient;

    @Autowired
    private DomainService domainService;

    @Autowired
    private IdentityUserService identityUserService;

    @Autowired
    private Configuration config;

    @Autowired
    private TenantService tenantService;

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
            if(user.isMultiFactorEnabled()) {
                // The user currently has mfa enabled, return an error.
                // If we replace the device now the user would have mfa enabled with an unvalidated phone
                String errMsg = "Cannot replace device with multifactor enabled";
                LOG.warn(errMsg);
                throw new IllegalStateException(errMsg);
            } else {
                MobilePhone originalPhone = mobilePhoneDao.getById(user.getMultiFactorMobilePhoneRsId());
                user.setMultiFactorDeviceVerified(null);
                user.setMultiFactorDevicePin(null);
                user.setMultiFactorDevicePinExpiration(null);
                MobilePhone newPhone = linkPhoneToUser(phoneNumber, user);
                unlinkPhoneFromUser(originalPhone, user);
                return newPhone;
            }
        } else {
            return linkPhoneToUser(phoneNumber, user);
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
        handleMultiFactorUnlock(user, multiFactor);
        handleMultiFactorEnable(user, multiFactor);
        handleMultiFactorUserEnforcementLevel(user, multiFactor);
    }

    private void handleMultiFactorUnlock(User user, MultiFactor multiFactor) {
        if (multiFactor.isUnlock() != null
                && multiFactor.isUnlock()
                && StringUtils.hasText(user.getExternalMultiFactorUserId())) {
            //want to try and unlock user regardless of mfa enable/disable
            userManagement.unlockUser(user.getExternalMultiFactorUserId());
            user.setMultiFactorState(MULTI_FACTOR_STATE_ACTIVE);
            userService.updateUserForMultiFactor(user);
        }
    }

    private void handleMultiFactorEnable(User user, MultiFactor multiFactor) {
        //only mess with enabling/disabling mfa on user if non-null
        if (multiFactor.isEnabled() != null && user.isMultiFactorEnabled() != multiFactor.isEnabled()) {
            if (!StringUtils.hasText(user.getMultiFactorMobilePhoneRsId())) {
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
    }

    private void handleMultiFactorUserEnforcementLevel(User user, MultiFactor multiFactor) {
        UserMultiFactorEnforcementLevelEnum existingLevel = userMultiFactorEnforcementLevelConverter.convertTo(user.getUserMultiFactorEnforcementLevel(), null);

        if (multiFactor.getUserMultiFactorEnforcementLevel() != null
                && multiFactor.getUserMultiFactorEnforcementLevel() != existingLevel) {
            user.setUserMultiFactorEnforcementLevel(userMultiFactorEnforcementLevelConverter.convertFrom(multiFactor.getUserMultiFactorEnforcementLevel()));
            userService.updateUserForMultiFactor(user);
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
        user.setMultiFactorState(null);
        userService.updateUserForMultiFactor(user);

        if (enabled) {
            atomHopperClient.asyncPost(user, AtomHopperConstants.MULTI_FACTOR);
            emailClient.asyncSendMultiFactorDisabledMessage(user);
        }

        if (StringUtils.hasText(phoneRsId)) {
            MobilePhone phone = mobilePhoneDao.getById(phoneRsId);
            unlinkPhoneFromUser(phone, user);
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
        userService.updateUserForMultiFactor(user);

        return mobilePhone;
    }

    /**
     * A utility method to remove a user from a mobile phone. This method will check to see if the mobile phone is now orphaned
     * and delete the phone if so. If any errors occur while trying to delete the phone from duo or the directory, the error will be caught and logged.
     *
     */
    private void unlinkPhoneFromUser(MobilePhone phone, User user) {
        try {
            if (isPhoneUserMembershipEnabled()) {
                //get and unlink phone
                phone.removeMember(user);
                //delete the phone from the directory and external provider is no other links to phone exist
                if(CollectionUtils.isEmpty(phone.getMembers())) {
                    mobilePhoneDao.deleteObject(phone);
                    try {
                        userManagement.deleteMobilePhone(phone.getExternalMultiFactorPhoneId());
                    } catch(MultiFactorException e) {
                        multiFactorConsistencyLogger.error(String.format("Error deleting phone '%s' from external provider. " +
                                "The phone has been removed from the directory but still exists in the external provider.", phone.getExternalMultiFactorPhoneId()));
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
        return Arrays.asList(userManagement.getBypassCodes(user.getExternalMultiFactorUserId(), normalizedNumberOfCodes, normalizedValidSecs));
    }

    private int getNumberOfCodes(BigInteger requested, boolean isSelfService) {
        if (!isSelfService) {
            return config.getBigInteger(BYPASS_DEFAULT_NUMBER, BigInteger.ONE).max(BigInteger.ONE).intValue();
        }
        final BigInteger max = config.getBigInteger(BYPASS_MAXIMUM_NUMBER, BigInteger.valueOf(10));
        if (requested == null) {
            return max.intValue();
        } else {
            return max.min(requested).max(BigInteger.ONE).intValue();
        }
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
        user.setMultiFactorState(MULTI_FACTOR_STATE_ACTIVE);
        userService.updateUserForMultiFactor(user);

        if (!alreadyEnabled) {
            revokeAllMFAProtectedTokensForUser(user);
            atomHopperClient.asyncPost(user, AtomHopperConstants.MULTI_FACTOR);
            emailClient.asyncSendMultiFactorEnabledMessage(user);
        }
    }

    /**
     * Revokes all the non-mfa tokens for the user, skipping those tokens issued by credentials that are NOT protected
     * via MFA.
     */
    private void revokeAllMFAProtectedTokensForUser(User user) {
        scopeAccessService.expireAllTokensExceptTypeForEndUser(user, AUTHENTICATEDBY_LIST_TO_NOT_REVOKE_ON_MFA_ENABLEMENT, false);
    }

    private void disableMultiFactorForUser(User user) {
        String providerUserId = user.getExternalMultiFactorUserId();

        boolean enabled = user.isMultiFactorEnabled();

        user.setMultifactorEnabled(false);
        user.setMultiFactorState(null);
        user.setExternalMultiFactorUserId(null);
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
        return config.getBoolean("multifactor.services.enabled", false);
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

    private boolean isMultiFactorBetaEnabled() {
        return config.getBoolean("multifactor.beta.enabled", false);
    }

    private boolean userHasMultiFactorBetaRole(BaseUser user) {
        List<TenantRole> userGlobalRoles = tenantService.getGlobalRolesForUser(user);

        if(userGlobalRoles != null && !userGlobalRoles.isEmpty()) {
            for(TenantRole role : userGlobalRoles) {
                if(role.getName().equals(config.getString(MULTIFACTOR_BETA_ROLE_NAME))) {
                    return true;
                }
            }
        }

        return false;
    }
}
