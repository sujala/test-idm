package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.EndUser;
import com.rackspace.idm.domain.entity.PhonePin;
import com.rackspace.idm.domain.entity.PhonePinProtectedUser;

import javax.xml.bind.JAXBException;
import java.io.IOException;

public interface PhonePinService {

    PhonePin resetPhonePin(PhonePinProtectedUser user);

    /**
     * Returns whether or not the specified pin is correct for the specified user.
     * @param userId
     * @param pin
     * @return true if the pin matches the user
     *
     * @throws com.rackspace.idm.exception.NotFoundException if the specified user does not exist
     * @throws com.rackspace.idm.exception.NoPinSetException If the specified user does not have a pin set
     */
    boolean verifyPhonePinOnUser(String userId, String pin);

    PhonePin checkAndGetPhonePin(PhonePinProtectedUser userId);

    String generatePhonePin();

    void unlockPhonePin(String user);
}
