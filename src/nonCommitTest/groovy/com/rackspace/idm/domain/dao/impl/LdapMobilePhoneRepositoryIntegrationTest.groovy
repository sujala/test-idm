package com.rackspace.idm.domain.dao.impl


import com.rackspace.idm.domain.dao.MobilePhoneDao
import com.rackspace.idm.domain.entity.Application
import com.rackspace.idm.domain.entity.MobilePhone
import com.unboundid.ldap.sdk.LDAPInterface
import com.unboundid.ldap.sdk.persist.LDAPPersister
import org.junit.Rule
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification
import testHelpers.EntityFactory

@ContextConfiguration(locations = "classpath:app-config.xml")
class LdapMobilePhoneRepositoryIntegrationTest extends Specification {

    @Shared def entityFactory = new EntityFactory()

    @Autowired
    private MobilePhoneDao mobilePhoneDao

    @Autowired
    LdapConnectionPools ldapConnectionPools

    LDAPInterface con
    LDAPPersister<Application> applicationPersister

    LdapIntegrationTestSupport ldapHelper;

    def setupSpec() {
    }

    def setup() {
        con = ldapConnectionPools.getAppConnPoolInterface()
        applicationPersister = LDAPPersister.getInstance(Application.class)
        ldapHelper = new LdapIntegrationTestSupport(con, MobilePhone.class)
    }

    def "Verify adding/retrieving a new mobile phone"() {
        setup:
        MobilePhone phone = entityFactory.createMobilePhoneWithId()
        String expectedDn =  getExpectedDn(phone)

        when:
        mobilePhoneDao.addObject(phone)
        MobilePhone retrievedPhone = mobilePhoneDao.getById(phone.getId())

        then: "Object should now be populated with ldap entry, entry should exist in ldap, and populated correctly"
        phone.getUniqueId() != null
        phone.getUniqueId() == expectedDn
        ldapHelper.entryExists(phone)
        retrievedPhone != null
        retrievedPhone.getExternalMultiFactorPhoneId() == null
        retrievedPhone.getId() == phone.getId()
        retrievedPhone.getTelephoneNumber() == phone.telephoneNumber

        cleanup:
        ldapHelper.deleteDirect(expectedDn) //delete this in case the ldap entry is not set correctly
        ldapHelper.deleteDirect(phone) //delete this in case the uniqueId is NOT the expected one
    }

    def "deleteObject(Obj) - verify can delete single object with no subentries"() {
        setup:
        MobilePhone phone = entityFactory.createMobilePhoneWithId()
        ldapHelper.addDirect(phone, LdapRepository.MULTIFACTOR_MOBILE_PHONE_BASE_DN)
        assert ldapHelper.entryExists(phone)

        when:
        mobilePhoneDao.deleteObject(phone)

        then:
        assert !ldapHelper.entryExists(phone)

        cleanup:
        ldapHelper.deleteDirect(phone) //in case delete through app does not work use SDK directly
    }

    /* ###############################################################################
                        TEST HELPER METHODS
    ############################################################################### */

    String getExpectedDn(MobilePhone phone) {
        new LdapRepository.LdapDnBuilder(LdapRepository.MULTIFACTOR_MOBILE_PHONE_BASE_DN).addAttribute(LdapRepository.ATTR_TELEPHONE_NUMBER, "\\" + phone.getTelephoneNumber()).build()
    }
}
