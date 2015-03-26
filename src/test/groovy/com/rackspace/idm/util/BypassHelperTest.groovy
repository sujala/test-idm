package com.rackspace.idm.util

import com.rackspace.idm.domain.entity.BypassDevice
import spock.lang.Shared
import spock.lang.Specification

class BypassHelperTest extends Specification {

    @Shared BypassHelper bypassHelper = new BypassHelper()

    def "test bypass code helpers"() {
        given:
        BypassDevice bypassDevice = bypassHelper.createBypassDevice(2, null)
        Set<String> bypassCodes = bypassDevice.getBypassCodes();

        when:
        def codes = bypassHelper.calculateBypassCodes(bypassDevice)

        then:
        codes.size() == bypassCodes.size()
        codes.containsAll(bypassCodes)
        codes.iterator().next().size() == 8
        bypassDevice.multiFactorDevicePinExpiration == null

        when:
        BypassDevice bypassCode2 = bypassHelper.createBypassDevice(1, 1)

        then:
        bypassCode2.multiFactorDevicePinExpiration != null
    }

}
