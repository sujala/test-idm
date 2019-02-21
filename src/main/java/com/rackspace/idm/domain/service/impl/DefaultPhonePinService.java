package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.ErrorCodes;
import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.IdentityUserDao;
import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.EndUser;
import com.rackspace.idm.domain.entity.PhonePin;
import com.rackspace.idm.domain.entity.PhonePinProtectedUser;
import com.rackspace.idm.domain.service.IdentityUserService;
import com.rackspace.idm.domain.service.PhonePinService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.NoPinSetException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.util.RandomGeneratorUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DefaultPhonePinService implements PhonePinService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private IdentityUserService identityUserService;

    @Autowired
    private IdentityConfig identityConfig;

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
        if (user.getPhonePin() == null) {
            audit.fail("User has not set a Phone PIN.");
            throw new NoPinSetException();
        }

        // A blank pin must never match a pin on a user
        if (StringUtils.isNotBlank(pin) && StringUtils.equals(pin, user.getPhonePin())) {
            audit.succeed();
            return true;
        }

        audit.fail("Incorrect Phone PIN provided.");
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
