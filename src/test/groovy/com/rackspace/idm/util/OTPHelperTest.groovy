package com.rackspace.idm.util

import com.rackspace.idm.domain.config.IdentityConfig
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class OTPHelperTest  extends Specification {

    @Shared OTPHelper otpHelper = new OTPHelper()

    def setupSpec() {
        otpHelper.config = Mock(IdentityConfig)
        def staticConfig = Mock(IdentityConfig.StaticConfig)
        otpHelper.config.getStaticConfig() >> staticConfig
        staticConfig.getOTPIssuer() >> "Rackspace"
    }

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
        dev1.key.length == 20
        dev2.key.length == 20
        Arrays.equals(dev1.key, dev2.key) == false

        "verified is set to false"
        dev1.multiFactorDeviceVerified == false
        dev2.multiFactorDeviceVerified == false
    }

    def "test URI generation"() {
        when:
        def uri = otpHelper.fromKeyToURI(new byte[20], "Test 1")

        then:
        uri != null
        uri == "otpauth://totp/Rackspace:Test%201?secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA&issuer=Rackspace"
    }

    def "test QR code generation"() {
        when:
        def qrcode = otpHelper.fromStringToQRCode(otpHelper.fromKeyToURI(new byte[32], "Test 1"))

        then:
        qrcode != null
        qrcode == "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAoAAAAKAAQAAAAAktvSOAAAD3ElEQVR42u3dO5LiMBCA4d4iIOQIPgpHg6P5KByB0AFFL1ivliybpWo2cM9PsFUe8OeNVGr1w6I//BFAQEBAQEBAQEBAQEBAQEDA/wdOUj6vi/NTTqqPfCFymX98fP97Fznobb7xaW47AwICugTj9fuXL/3P+zax4EMGDd+oXt/gW5+fWwOAgIDewHnlOERQwi9vcSVJS48G4zr/8WguAAEBfwd4jhuO1/cSFpjwYwnr0GWhAwIC/gZQdZyNuPuY9Us0xwx+sdoAAgLuGbRxSjCq2+yjqq3IPwU+gICAuwWXJ5zXtC/ZuvjmyBQQEHB/4MonLDDxjkHDbSZO+TrZCggIuDMw5TXEnE7McUpIh1a7j57eW74AAQF9gGJjjiYXMhdO3Ap4iCcaZfvSraoCBAR0AJZDjPfK0c+Ahm9MFYWEoOXDqQggIOB+QQ21lJeyL7nbDUfKhcQqyzY3OgACAvoFTV6jlEeU3KjYCOZqSyoCuDwVAQQE9AHWK0epiFATmqTH5aAlP6qX/gAEBHQAxp1EKo/QUnJ5zOtQOO7MbV2m4WtYDXwAAQH3D9pmzrGsKbFZo1p6UpwSfvaw4QwgIKA7MGdAzVZkyMcbUjYpxZBYti2SUqiAgIDOwHnliLnRsNosB0TMocncrJFyo1qFM4CAgA7BFHNobslo45RD3pfEqqr0n+inPwABAb2ApqoqNXM2AyLaKsvjVm4UEBDQB6i5ZfMmiz3Gsy2srHOjh5XuD0BAQAdgs9poKaSaTFVVffb5ITcKCAjoAyy1U+3kh1I71am30q1kKyAgoANQcp4z118eTA/4xcYpYhMj6R4FBAR0CZbcaHWOWY+3t1nTdPapj62SS0BAQBdg6tca8zjrah3K0yLirPtRzEQ6BQQEdAg2nVxmoJwdSllvRSabKP3Q5gkICLhTcOp3end7wE+L5wICAnoGcxdGrq1e9oBXg++n7dwoICCgD9DkNUotpZlQWeqxzfv1mkcBAgK6A5uqqjBtsukBFzEll8OHHnBAQEA3oKbyiHpIba8eO9VfjouzT0BAQGfgZMdATIuBcvf85r3Sn1ElUdfqsQEBAXcOVp+yx6h6wNO+pPR4nVV1NTcKCAjoAZzqlWOjBzyfdeSR+CvD8wEBAX2A8dq8VXMZp1RZElOmOQACAjoGm7dm9XrAy1GFqrajZBQQENA7qOblm/XLtVb2Jf1mDUBAQH/gKG0PuOnXSlVVVZvnBAgI6BW0cYo0PeCnulK7kxsVQEBAp6A54axevnns5EbTJmVzeD4gIKAH8Oc+gICAgICAgICAgICAgICAgLsA/wLuavZ9wLhrIAAAAABJRU5ErkJggg=="
    }

    def "test HOTP keys"() {
        given:
        def key = "12345678901234567890".getBytes();
        def hotp_result_vector = ["755224", "287082", "359152", "969429", "338314",
                                  "254676", "287922", "162583", "399871", "520489"];

        def key2 = new byte[32]
        def hotp_8_result_vector = ["35328482", "30812658", "41073348", "81887919", "72320986",
                                    "76435986", "12964213", "15267638", "12985814", "60003773"];

        when: "test 6 digits"
        def errors = []
        for (int i=0; i<hotp_result_vector.size(); i++) {
            def expect = hotp_result_vector[i];
            def result = otpHelper.HOTP(key, i);
            if (expect != result) errors.add(expect  + " != " + result)
        }

        then:
        errors == []

        when: "test 8 digits"
        def errors2 = []
        for (int i=0; i<hotp_8_result_vector.size(); i++) {
            def expect = hotp_8_result_vector[i];
            def result = otpHelper.HOTP(key2, i, 8);
            if (expect != result) errors2.add(expect  + " != " + result)
        }

        then:
        errors2 == []
    }

    @Unroll
    def "test TOTP keys (digits: #digits)"() {
        given:
        def key = "12345678901234567890".getBytes();

        when:
        if ((((int) (System.currentTimeMillis() / 1000)) % 30) < 3) {
            Thread.sleep(4000) // avoid race test on the time shift
        }

        def key1 = otpHelper.TOTP(key, digits)
        def ts = (int) ((System.currentTimeMillis() / 1000) / 30) - 1
        def key2 = otpHelper.HOTP(key, ts, digits)

        then:
        key1 == key2

        where:
        digits | _
        6      | _
        7      | _
        8      | _
    }

}
