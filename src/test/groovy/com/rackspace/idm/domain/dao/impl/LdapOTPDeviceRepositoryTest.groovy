package com.rackspace.idm.domain.dao.impl

import com.rackspace.idm.domain.dao.OTPDeviceDao
import com.rackspace.idm.domain.dao.UserDao
import com.rackspace.idm.domain.entity.OTPDevice
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.util.OTPHelper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

@ContextConfiguration(locations = "classpath:app-config.xml")
class LdapOTPDeviceRepositoryTest extends Specification {

    @Autowired
    def OTPDeviceDao otpDeviceDao

    @Autowired
    def UserDao userDao

    @Autowired
    def OTPHelper otpHelper

    def "test creation of an OTP device"() {
        given:
        OTPDevice device = otpHelper.createOTPDevice("Sample key")
        User user = userDao.getUserByUsername("auth")

        when:
        otpDeviceDao.addOTPDevice(user, device)

        then:
        device.id != null

        when:
        def dev = otpDeviceDao.getOTPDeviceByParentAndId(user, device.id)

        then:
        device.name == dev.name

        cleanup:
        otpDeviceDao.deleteOTPDevice(device)
    }
}
