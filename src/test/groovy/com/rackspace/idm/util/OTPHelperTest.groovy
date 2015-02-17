package com.rackspace.idm.util

import spock.lang.Shared
import spock.lang.Specification

class OTPHelperTest  extends Specification {

    @Shared OTPHelper otpHelper = new OTPHelper()

    def "test create OTP device"() {
        when:
        def dev1 = otpHelper.createOTPDevice("d1")
        def dev2 = otpHelper.createOTPDevice("d2")

        then:
        "ids are created and different from each other"
        dev1.id != null
        dev2.id != null
        dev1.id.length() == 32
        dev2.id.length() == 32
        dev1.id != dev2.id

        "keys are created and different from each other"
        dev1.key != null
        dev2.key != null
        dev1.key.length == 32
        dev2.key.length == 32
        Arrays.equals(dev1.key, dev2.key) == false

        "verified is set to false"
        dev1.multiFactorDeviceVerified == false
        dev2.multiFactorDeviceVerified == false
    }

    def "test URI generation"() {
        when:
        def uri = otpHelper.fromKeyToURI(new byte[32], "Test 1")

        then:
        uri != null
        uri == "otpauth://totp/Rackspace:Test%201?secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA&issuer=Rackspace"
    }

    def "test QR code generation"() {
        when:
        def qrcode = otpHelper.fromStringToQRCode(otpHelper.fromKeyToURI(new byte[32], "Test 1"))

        then:
        qrcode != null
        qrcode == "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAoAAAAKAAQAAAAAktvSOAAAD00lEQVR42u3dO27rMBBAUQYuUmYJWoqXZi/NS/ESXLoQMk8fkjNDyUoC5BWcXDWBIPkoFUHOh0zyy1cCBAQEBAQEBAQEBAQEBAQE/H/gM+k137zJo96cP6e/l+Xld7ktT05yX374aX52BgQEDAnm+/nNRf+QqwPHNMh6Mz2ZwUmX+aYBAAEBo4HLyHGq4DLA3PNIMv1sAWU1lk8VsH4XEBDwD4B5wjE9T/NNWYPkaxp6UnI6ICDgXwBFbsnOPmTS16GnrFN+ONoAAgL2DLp1yvZn5VPz0OOmIt9b+AACAvYKuginhiq+uvlmyBQQELBTsL0285LpGvKzkhj5UbIVEBCwT/C5hiqu+U015gmH+NlHeZLyCmZ8MXwBAgLGADXJYeMWPhfynj91Wn910+nLblUVICBg92COTohNh24zoGsu5FFjn+trp70gBiAgYBjQlUfk2UeZcMj8JNUqyzY3OgACAsYFZRO69LnR0WVATUnFNZl/AhAQMBiY31ymFWt5hEY0SrizadZYwxuP42YNQEDA/kHNc+rPnL7OS6peEqUm8AEICBgOzEnPS16nLGDbrKFLk3KjlZn7hROAgIDdg03J5Vn8AuSRCyfsAFPbOMYcCAUEBAwIauhysIlSt0HE3U9SfCeXAAICBgXX1YjbFmYT0ajrlDwO3dJRPgUQEDAAqBvGlCrLx84GEQW8jL6kAhAQMDB4/vSzj48m/ZFe5kZP+80agICAEcByjWaOIU3hxDIO3Wxx9kFuFBAQMAZoO7k0WlmfPGqSw9Vjn/WfeAICAoYEXZ6zdHZuesDNADNs8qkCCAgYE3yrVddSd5drdV3BDDX2KeP+3AYQEDAMKNqymTeUcz3gJhdyqg3huiOdAAICBgS1kKq97KaUPjeqe9UNL3e5BAQE7BzUEmypb/pC6+1BGI0OCAgYFEymIqIsQIa9HnDz3aPcKCAgYASwadlMRz3guk7R3OhBVRUgIGDPoPiHaa8HPFXDncR52AMOCAjYP6h5jWSbt2w9tv/uXj4VEBAwGFhqp6TGMT+aHnDt8dLvuusMCAgYEBRfZdl2brjcqPmuLb4SQEDAgKA7X08HmO2+c644uxy2J18cvgkICNgxWPo3782GMWnv/By7t/X1qBIDEBCwf1DP23yaU3La9Iee1G3Gp+FFbhQQEDAWKHuFE03Z9t2ep/Uq2QoICBgN1DHF9ICXOKbqPlEKCAgYE7TrlGRbtEwPeDLnZchP9roHBATsFjQRTtui5XrA6zrFVma+2jwfEBAwAvh7FyAgICAgICAgICAgICAgIGAX4D+H7ZE8pWd2twAAAABJRU5ErkJggg=="
    }

}
