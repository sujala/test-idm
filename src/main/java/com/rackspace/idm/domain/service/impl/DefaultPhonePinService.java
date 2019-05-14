package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.ErrorCodes;
import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.dao.IdentityUserDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.IdentityUserService;
import com.rackspace.idm.domain.service.PhonePinService;
import com.rackspace.idm.exception.NoPinSetException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.exception.PhonePinLockedException;
import com.rackspace.idm.util.RandomGeneratorUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DefaultPhonePinService implements PhonePinService {
    public static final String PHONE_PIN_NOT_SET_AUDIT_MSG = "User has not set a Phone PIN.";
    public static final String PHONE_PIN_INCORRECT_NOW_LOCKED_AUDIT_MSG = "Incorrect Phone PIN provided. Maximum attempts reached. Phone PIN is now locked.";
    public static final String PHONE_PIN_LOCKED_AUDIT_MSG = "Phone PIN is locked.";
    public static final String PHONE_PIN_INCORRET_PIN_AUDIT_MSG = "Incorrect Phone PIN provided.";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private IdentityUserService identityUserService;

    @Autowired
    private IdentityUserDao identityUserDao;

    @Override
    public PhonePin resetPhonePin(PhonePinProtectedUser user) {
        user.setPhonePin(generatePhonePin());

        identityUserDao.updateIdentityUser((BaseUser) user);

        return checkAndGetPhonePin(user);
    }

    @Override
    public boolean verifyPhonePinOnUser(String userId, String pin) {
        EndUser user = identityUserService.checkAndGetEndUserById(userId);

        Audit audit = Audit.verifyPhonePin(user);
        if (user.getPhonePinState() == PhonePinStateEnum.INACTIVE) {
            audit.fail(PHONE_PIN_NOT_SET_AUDIT_MSG);
            throw new NoPinSetException();
        }

        if (user.getPhonePinState() == PhonePinStateEnum.LOCKED) {
            audit.fail(PHONE_PIN_LOCKED_AUDIT_MSG);
            throw new PhonePinLockedException();
        }

        // A blank pin must never match a pin on a user
        if (StringUtils.isNotBlank(pin) && StringUtils.equals(pin, user.getPhonePin())) {
            audit.succeed();

            // Record the success
            user.recordSuccessfulPinAuthentication();
            identityUserService.updateEndUser(user);
            return true;
        }

        // Record failure
        user.recordFailedPinAuthentication();

        // If this failure causes the phone pin to become locked, need distinct error message. Record in audit before saving.
        if (user.getPhonePinState() == PhonePinStateEnum.LOCKED) {
            audit.fail(PHONE_PIN_INCORRECT_NOW_LOCKED_AUDIT_MSG);
        } else {
            audit.fail(PHONE_PIN_INCORRET_PIN_AUDIT_MSG);
        }

        // Update the user
        identityUserService.updateEndUser(user);

        return false;
    }

    @Override
    public PhonePin checkAndGetPhonePin(PhonePinProtectedUser user) {
        if(StringUtils.isBlank(user.getPhonePin())) {
            String errMsg = String.format("Phone pin not found the user.");
            logger.warn(errMsg);
            throw new NotFoundException(errMsg, ErrorCodes.ERROR_CODE_PHONE_PIN_NOT_FOUND);
        }
        PhonePin phonePin = new PhonePin();
        phonePin.setPin(user.getPhonePin());
        return phonePin;
    }

    /**
     * This method generates and returns 6 digit pin.
     *
     * @return String - 6 digit phone pin
     */
    @Override
    public String generatePhonePin() {
        StringBuilder phonePin = new StringBuilder();

        // getting first 2 digits of phone pins
        String firstPart = RandomGeneratorUtil.generateSecureRandomNumber(2);
        phonePin.append(firstPart);

        for (int i = 2; i < 6; i++) {
            String digit3 = RandomGeneratorUtil.getNextPhonePinDigit(phonePin.charAt(i - 2), phonePin.charAt(i - 1));
            phonePin.append(digit3);
        }

        return phonePin.toString();
    }

}
