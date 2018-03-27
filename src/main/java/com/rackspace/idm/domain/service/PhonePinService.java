package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.EndUser;
import com.rackspace.idm.domain.entity.PhonePin;
import com.rackspace.idm.domain.entity.PhonePinProtectedUser;

import javax.xml.bind.JAXBException;
import java.io.IOException;

public interface PhonePinService {

    PhonePin resetPhonePin(PhonePinProtectedUser user);
    void verifyPhonePin(PhonePinProtectedUser user, String pin);
    PhonePin checkAndGetPhonePin(PhonePinProtectedUser userId);
}
