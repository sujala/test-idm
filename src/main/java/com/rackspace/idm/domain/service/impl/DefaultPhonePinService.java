package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.ErrorCodes;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.IdentityUserDao;
import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.PhonePin;
import com.rackspace.idm.domain.entity.PhonePinProtectedUser;
import com.rackspace.idm.domain.service.IdentityUserService;
import com.rackspace.idm.domain.service.PhonePinService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.util.RandomGeneratorUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
        user.setPhonePin(RandomGeneratorUtil.generateSecureRandomNumber
                (identityConfig.getReloadableConfig().getUserPhonePinSize()));
        identityUserDao.updateIdentityUser((BaseUser) user);

        return checkAndGetPhonePin(user);
    }

    @Override
    public void verifyPhonePin(PhonePinProtectedUser user, String pin) {
        if (!StringUtils.equals(pin, checkAndGetPhonePin(user).getPin())) {
            throw new BadRequestException(String.format("Incorrect phone pin for the user."),
                    ErrorCodes.ERROR_CODE_PHONE_PIN_BAD_REQUEST);
        }
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
}
