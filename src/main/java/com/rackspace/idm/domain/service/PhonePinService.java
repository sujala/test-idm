package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.EndUser;
import com.rackspace.idm.domain.entity.PhonePin;

import javax.xml.bind.JAXBException;
import java.io.IOException;

public interface PhonePinService {

    PhonePin resetPhonePin(EndUser user) throws IOException, JAXBException;
    void verifyPhonePin(EndUser user, String pin) throws IOException, JAXBException;
    PhonePin checkAndGetPhonePin(EndUser user);
}
