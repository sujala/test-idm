package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.ErrorCodes;
import com.rackspace.idm.domain.entity.EndUser;
import com.rackspace.idm.domain.entity.FederatedUser;
import com.rackspace.idm.domain.entity.PhonePin;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.PhonePinService;
import com.rackspace.idm.exception.NotFoundException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBException;
import java.io.IOException;

@Component
public class DefaultPhonePinService implements PhonePinService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public PhonePin resetPhonePin(EndUser user) throws IOException, JAXBException {
        // Not Implemented
        return null;
    }

    @Override
    public void verifyPhonePin(EndUser user, String pin) throws IOException, JAXBException {
        // Not Implemented
    }

    @Override
    public PhonePin checkAndGetPhonePin(EndUser user) {
        PhonePin phonePin = new PhonePin();

        if(user instanceof User) {
            phonePin.setPin(((User) user).getPhonePin());
        } else if(user instanceof FederatedUser) {
            phonePin.setPin(((FederatedUser) user).getPhonePin());
        } else {
            throw new IllegalStateException(String.format("Unknown user type '%s'", user.getClass().getName()));
        }

        if(StringUtils.isBlank(phonePin.getPin())) {
            String errMsg = String.format("Phone pin not found for userId: %s ", user.getId());
            logger.warn(errMsg);
            throw new NotFoundException(errMsg, ErrorCodes.ERROR_CODE_PHONE_PIN_NOT_FOUND);
        }
        return phonePin;
    }
}
