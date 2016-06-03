import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.rackspace.idm.multifactor.PhoneNumberGenerator
import spock.lang.Specification
import spock.lang.Unroll

class PhoneNumberGeneratorTest extends Specification {

    @Unroll
    def "test area codes are valid for US: areaCode = #areaCode"() {
        given:
        def phoneNumberUtil = PhoneNumberUtil.getInstance();

        when:
        def numberString = "" + areaCode + "5550111"
        def number = phoneNumberUtil.parse(numberString, "US");

        then:
        phoneNumberUtil.isValidNumber(number)

        where:
        areaCode << PhoneNumberGenerator.areaCodes
    }

}
