package com.rackspace.idm.multifactor.service

import com.rackspace.idm.domain.dao.MobilePhoneDao
import spock.lang.Shared
import spock.lang.Specification
import testHelpers.RootServiceTest


class BasicMultiFactorServiceTest extends RootServiceTest {

    @Shared BasicMultiFactorService service

    def setupSpec() {
        service = new BasicMultiFactorService()
    }

    def setup() {
        mockMobilePhoneRepository(service)
    }

    def "listing mobile phones returns empty list if not defined"() {
        given:
        def user = entityFactory.createUser().with {
            it.multiFactorMobilePhoneRsId = null
            it
        }

        when:
        def result = service.getMobilePhonesForUser(user)

        then:
        result != null
        result.size() == 0
    }

    def "listing mobile phones returns device if defined"() {
        given:
        def user = entityFactory.createUser().with {
            it.multiFactorMobilePhoneRsId = "id"
            it
        }
        def mobilePhone = entityFactory.createMobilePhone()

        when:
        def result = service.getMobilePhonesForUser(user)

        then:
        1 * mobilePhoneDao.getById(_) >> mobilePhone
        result != null
        result.size() == 1
        result.get(0).telephoneNumber == mobilePhone.telephoneNumber
    }

    def "listing mobile phones returns empty list even if directory inconsistent"() {
        given:
        def user = entityFactory.createUser().with {
            it.multiFactorMobilePhoneRsId = "id"
            it
        }
        def mobilePhone = entityFactory.createMobilePhone()

        when:
        def result = service.getMobilePhonesForUser(user)

        then:
        1 * mobilePhoneDao.getById(_) >> null
        result != null
        result.size() == 0
    }

}
