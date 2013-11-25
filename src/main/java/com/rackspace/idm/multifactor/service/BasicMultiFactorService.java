package com.rackspace.idm.multifactor.service;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.rackspace.idm.domain.dao.impl.LdapMobilePhoneRepository;
import com.rackspace.idm.domain.dao.impl.LdapUserRepository;
import com.rackspace.idm.domain.entity.MobilePhone;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.InvalidPhoneNumberException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.exception.SaveOrUpdateException;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.xml.bind.JAXBException;
import java.io.IOException;

/**
 * Simple implementation that stores mobile phones in the ldap directory, and integrates with a single external provider with no fallback.
 */
@Component
public class BasicMultiFactorService implements MultiFactorService {
    private static final Logger LOG = LoggerFactory.getLogger(BasicMultiFactorService.class);

    @Autowired
    private LdapMobilePhoneRepository mobilePhoneRepository;

    @Autowired
    private UserService userService;

    private PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();

    @Override
    public MobilePhone addPhoneToUser(String userId, Phonenumber.PhoneNumber phoneNumber) {
        Assert.notNull(phoneNumber);
        Assert.notNull(userId);

        User user = userService.checkAndGetUserById(userId);
        if (user.getMultiFactorMobilePhoneRsId() != null) {
            String errMsg = "User is already associated with a mobile phone";
            LOG.warn(errMsg);
            throw new IllegalStateException(errMsg);
        }

        MobilePhone mobilePhone = null;
        try {
            mobilePhone = createMobilePhone(phoneNumber);
        } catch (DuplicateException e) {
            //phone number exists already, retrieve it to link user to it
            mobilePhone = mobilePhoneRepository.getByTelephoneNumber(canonicalizePhoneNumberToString(phoneNumber));
            if (mobilePhone == null) {
                throw new IllegalStateException("Mobile phone exists but could not be found");
            }
        }

        //now link user to the created/retrieved phone
        user.setMultiFactorMobilePhoneRsId(mobilePhone.getId());
        try {
            userService.updateUser(user, false);
        } catch (IOException e) {
            //The interface declares it so have to catch it here... don't want to bubble it up cause it makes no business sense to do so
            String errMsg = String.format("Error updating user %s", userId);
            throw new SaveOrUpdateException(errMsg, e);
        } catch (JAXBException e) {
            //The interface declares it so have to catch it here... don't want to bubble it up cause it makes no business sense to do so.
            // sure would be nice to have java7 so could collapse this exception block with the previous...
            String errMsg = String.format("Error updating user %s", userId);
            throw new SaveOrUpdateException(errMsg, e);
        }

        return mobilePhone;
    }

    @Override
    public String canonicalizePhoneNumberToString(Phonenumber.PhoneNumber phoneNumber) {
        return phoneNumberUtil.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL);
    }

    @Override
    public Phonenumber.PhoneNumber parsePhoneNumber(String phoneNumber) {
        try {
            return phoneNumberUtil.parse(phoneNumber, MobilePhone.TELEPHONE_DEFAULT_REGION);
        } catch (NumberParseException e) {
            throw new InvalidPhoneNumberException("The phone number '" + phoneNumber + "' does not appear to be a valid phone number", e);
        }
    }

    private MobilePhone createMobilePhone(Phonenumber.PhoneNumber phoneNumber) {
        Assert.notNull(phoneNumber);
        String canonicalizedPhone = canonicalizePhoneNumberToString(phoneNumber);

        MobilePhone mobilePhone = new MobilePhone();
        mobilePhone.setTelephoneNumber(canonicalizedPhone);
        mobilePhone.setId(mobilePhoneRepository.getNextId());

        mobilePhoneRepository.addObject(mobilePhone);
        return mobilePhone;
    }
}
