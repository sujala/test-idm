package com.rackspace.idm.api.converter.cloudv20

import com.rackspace.idm.util.OTPHelper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

@ContextConfiguration(locations = "classpath:app-config.xml")
class OTPDeviceConverterCloudV20Test extends Specification {

    @Autowired
    OTPHelper otpHelper

    @Autowired
    OTPDeviceConverterCloudV20 converterCloudV20

    def "test if toOTPDeviceForWeb don't copy the key"() {
        given:
        def entity = otpHelper.createOTPDevice("test")

        when:
        def data = converterCloudV20.toOTPDeviceForWeb(entity)

        then:
        data.verified == entity.multiFactorDeviceVerified
        data.name == entity.name
        data.id != null
        data.keyUri == null
        data.qrcode == null
    }

    def "test if toOTPDeviceForCreate creates the key URI and qrcode"() {
        given:
        def entity = otpHelper.createOTPDevice("test")

        when:
        def data = converterCloudV20.toOTPDeviceForCreate(entity)

        then:
        data.verified == entity.multiFactorDeviceVerified
        data.name == entity.name
        data.id != null
        data.keyUri != null
        data.qrcode != null
    }

}
