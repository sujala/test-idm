package com.rackspace.idm.util

import com.rackspace.idm.domain.config.IdentityConfig
import org.joda.time.DateTime
import spock.lang.Specification
import spock.lang.Unroll

class BypassHelperTest extends Specification {

    BypassHelper bypassHelper = new BypassHelper()
    def reloadConfig
    def staticConfig

    def setup() {
        bypassHelper.identityConfig = Mock(IdentityConfig)
        staticConfig = Mock(IdentityConfig.StaticConfig)
        reloadConfig = Mock(IdentityConfig.ReloadableConfig)
        bypassHelper.identityConfig.getReloadableConfig() >> reloadConfig
        bypassHelper.identityConfig.getStaticConfig() >> staticConfig
    }

    @Unroll
    def "test bypass code device creation for numCodes: #numCodes, expiration: #expirationInSeconds, iterations: #iterations"() {
        given:
        DateTime start = new DateTime()
        reloadConfig.getLocalBypassCodeIterationCount() >> iterationCount

        when:
        BypassDeviceCreationResult bypassDeviceCreationResult = bypassHelper.createBypassDevice(numCodes, expirationInSeconds)
        DateTime end = new DateTime()

        then: "bypass device is created"
        bypassDeviceCreationResult.device != null
        bypassDeviceCreationResult.device.bypassCodes.size() == numCodes
        bypassDeviceCreationResult.device.salt != null
        bypassDeviceCreationResult.device.iterations == iterationCount

        if (expirationInSeconds != null) {
            DateTime minBypassExpiration = start.plusSeconds(expirationInSeconds)
            DateTime maxBypassExpiration = end.plusSeconds(expirationInSeconds)
            def actualExpiration = new DateTime(bypassDeviceCreationResult.device.multiFactorDevicePinExpiration)
            (actualExpiration.isAfter(minBypassExpiration) || actualExpiration.isEqual(minBypassExpiration)) &&
                    (actualExpiration.isBefore(maxBypassExpiration) || actualExpiration.isEqual(maxBypassExpiration))
        } else {
            bypassDeviceCreationResult.device.multiFactorDevicePinExpiration != null
        }

        and: "plaintext codes are provided"
        bypassDeviceCreationResult.plainTextBypassCodes.size() == numCodes

        //the plaintext codes must not be as-is in the device
        for (String plainTextCode : bypassDeviceCreationResult.plainTextBypassCodes) {
            bypassDeviceCreationResult.device.bypassCodes.find()
                    {it == plainTextCode} == null
        }

        and: "plaintext codes can be encoded to what is provided in device"
        for (String plainTextCode : bypassDeviceCreationResult.plainTextBypassCodes) {
            bypassDeviceCreationResult.device.bypassCodes.find()
                    {it == bypassHelper.encodeCodeForDevice(bypassDeviceCreationResult.device, plainTextCode)} != null
        }

        where:
        numCodes | expirationInSeconds | iterationCount
        2 | null | 10
        4 | null | 20
        3 | 10 | 30
    }
}
