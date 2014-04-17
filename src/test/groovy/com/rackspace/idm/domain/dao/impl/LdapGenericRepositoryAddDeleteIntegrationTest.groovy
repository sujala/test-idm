package com.rackspace.idm.domain.dao.impl

import com.rackspace.idm.domain.entity.Application
import com.rackspace.idm.domain.entity.ClientSecret
import com.rackspace.idm.exception.DuplicateException
import com.rackspace.test.SingleTestConfiguration
import com.unboundid.ldap.sdk.Attribute
import com.unboundid.ldap.sdk.DeleteRequest
import com.unboundid.ldap.sdk.LDAPInterface
import com.unboundid.ldap.sdk.controls.SubtreeDeleteRequestControl
import com.unboundid.ldap.sdk.persist.LDAPPersister
import org.apache.commons.configuration.Configuration
import org.apache.commons.lang.NotImplementedException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.test.context.ContextConfiguration
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Specification

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

/**
 * This test is used to test the add/delete functions of the LdapGenericRepository/LdapRepository and the integration with the unboundid sdk. It uses the Application object as the
 * persisted object for tests. Tests attempt to clean up after themselves.
 *
 */
@ContextConfiguration(locations = ["classpath:app-config.xml", "classpath:com/rackspace/idm/domain/dao/impl/LdapGenericRepositoryAddDeleteIntegrationTest-context.xml"])
class LdapGenericRepositoryAddDeleteIntegrationTest extends Specification {

    /**
     * Random string generated for entire test class. Same for all feature methods.
     */
    private static final String TESTTOKENS_CONTAINER_NAME = "TESTTOKENS"
    @Shared String SPECIFICATION_RANDOM

    /**
     * Random string that is unique for each feature method
     */
    @Shared String FEATURE_RANDOM

    /**
     * Wire in the generic repo
     */
    @Autowired
    LdapGenericRepository<Application> genericApplicationRepository

    /**
     * Wire in the overridden generic repo
     */
    @Autowired
    LdapGenericRepository<Application> overriddenGenericApplicationRepository

    @Autowired
    LdapConnectionPools ldapConnectionPools

    @Autowired
    Configuration config

    LDAPInterface con
    LDAPPersister<Application> applicationPersister

    def setupSpec() {
        SPECIFICATION_RANDOM = UUID.randomUUID().toString().replaceAll('-', "")
    }

    def setup() {
        FEATURE_RANDOM = UUID.randomUUID().toString().replaceAll('-', "")
        con = ldapConnectionPools.getAppConnPoolInterface()
        applicationPersister = LDAPPersister.getInstance(Application.class)
    }

    /* ###############################################################################
                        addObject(Object) tests
    ############################################################################### */
    def "addObject(Obj) - verify adding new object when subclass provides base dn"() {
        setup:
        Application app = createClient()
        String expectedDn =  getExpectedClientDn(app)

        when:
        overriddenGenericApplicationRepository.addObject(app)

        then: "Application object should now be populated with ldap entry and entry should exist in ldap"
        app.getLdapEntry() != null
        app.getUniqueId() == expectedDn
        con.getEntry(app.getUniqueId()) != null

        cleanup:
        deleteDirect(expectedDn) //delete this in case the ldap entry is not set correctly
        deleteDirect(app) //delete this in case the uniqueId is NOT the expected one
    }

    def "addObject(Obj) - verify throws NotImplementedException when add new object and subclass does not override getBaseDN"() {
        setup:
        Application app = createClient()
        String expectedDn =  getExpectedClientDn(app)

        when:
        genericApplicationRepository.addObject(app)

        then: "Throw exception"
        thrown(NotImplementedException)

        cleanup: "Just in case object was actually created"
        deleteDirect(expectedDn) //delete this in case the ldap entry is not set correctly
        deleteDirect(app) //delete this in case the uniqueId is NOT the expected one
    }

    def "addObject(Obj) - verify throws NotImplementedException when provide null argument"() {
        when:
        genericApplicationRepository.addObject(null)

        then: "Throw exception"
        thrown(NotImplementedException)
    }

    def "addObject(Obj) - verify adding same object twice throws DuplicateException"() {
        setup:
        Application app = createClient()
        String expectedDn = getExpectedClientDn(app)
        overriddenGenericApplicationRepository.addObject(app)
        assert con.getEntry(app.getUniqueId()) != null

        when: "add a second time"
        overriddenGenericApplicationRepository.addObject(app)

        then: "Application throws exception"
        thrown(DuplicateException)

        cleanup:
        deleteDirect(expectedDn) //delete this in case the ldap entry is not set correctly
        deleteDirect(app) //delete this in case the uniqueId is NOT the expected one
    }

    private String getExpectedClientDn(Application app) {
        new LdapRepository.LdapDnBuilder(LdapRepository.APPLICATIONS_BASE_DN).addAttribute("clientId", app.getClientId()).build()
    }

    /* ###############################################################################
                        addObject(String dn, T Object) tests
    ############################################################################### */
    def "addObject(String, Obj) - verify throws IllegalArgumentException when provide null obj argument"() {
        when:
        genericApplicationRepository.addObject("abc", null)

        then: "Throw exception"
        thrown(IllegalArgumentException)
    }

    def "addObject(String, Obj) - verify adding new object when provided dn"() {
        setup:
        Application app = createClient()
        String expectedDn =  getExpectedClientDn(app)

        when:
        genericApplicationRepository.addObject(LdapRepository.APPLICATIONS_BASE_DN, app)

        then: "Application object should now be populated with ldap entry and entry should exist in ldap"
        app.getLdapEntry() != null
        app.getUniqueId() == expectedDn
        con.getEntry(app.getUniqueId()) != null

        cleanup:
        deleteDirect(expectedDn) //delete this in case the ldap entry is not set correctly
        deleteDirect(app) //delete this in case the uniqueId is NOT the expected one
    }

    def "addObject(String, Obj) - verify adding same object twice throws DuplicateException"() {
        setup:
        Application app = createClient()
        String expectedDn =  getExpectedClientDn(app)
        genericApplicationRepository.addObject(LdapRepository.APPLICATIONS_BASE_DN, app)
        assert con.getEntry(app.getUniqueId()) != null

        when: "add a second time"
        genericApplicationRepository.addObject(LdapRepository.APPLICATIONS_BASE_DN, app)

        then: "Application throws exception"
        thrown(DuplicateException)

        cleanup:
        deleteDirect(expectedDn) //delete this in case the ldap entry is not set correctly
        deleteDirect(app) //delete this in case the uniqueId is NOT the expected one
    }

    /* ###############################################################################
                        deleteObject(Object) tests
    ############################################################################### */

    def "deleteObject(Obj) - verify can delete single object with no subentries using recursion subtree delete"() {
        setup:
        assert !genericApplicationRepository.useSubtreeDeleteControlForSubtreeDeletion()
        Application app = persistClientDirect()
        assert con.getEntry(app.getUniqueId()) != null

        when:
        genericApplicationRepository.deleteObject(app)

        then:
        con.getEntry(app.getUniqueId()) == null

        cleanup:
        deleteDirect(app)
    }

    def "deleteObject(Obj) - verify throws exception if delete object that no longer exists using recursion subtree delete"() {
        setup:
        assert !genericApplicationRepository.useSubtreeDeleteControlForSubtreeDeletion()
        Application app = persistClientDirect()
        genericApplicationRepository.deleteObject(app) //delete the object from ldap
        con.getEntry(app.getUniqueId()) == null

        when:
        genericApplicationRepository.deleteObject(app) //trying to delete the object should throw an exception

        then:
        def ex = thrown(IllegalStateException)
        ex.message == "no such object"

        cleanup:
        deleteDirect(app)
    }

    def "deleteObject(Obj) - verify throws exception if delete object that never existed using recursion subtree delete"() {
        setup:
        assert !genericApplicationRepository.useSubtreeDeleteControlForSubtreeDeletion()
        Application app = mock(Application.class)
        when(app.getUniqueId()).thenReturn("ou=nonexistant,o=rackspace,dc=rackspace,dc=com")

        when:
        genericApplicationRepository.deleteObject(app) //trying to delete the object should throw an exception

        then:
        def ex = thrown(IllegalStateException)
        ex.message == "no such object"
    }

    def "deleteObject(Obj) - verify deleting entry that has children will delete entire tree using recursion subtree delete"() {
        setup:
        assert !genericApplicationRepository.useSubtreeDeleteControlForSubtreeDeletion()
        Application app = persistClientDirect()
        addTokenContainerDirect(app)
        assert con.getEntry(app.getUniqueId()) != null
        assert con.getEntry(getTokenContainerDNForClient(app)) != null

        when:
        genericApplicationRepository.deleteObject(app)

        then:
        assert con.getEntry(app.getUniqueId()) == null
        assert con.getEntry(getTokenContainerDNForClient(app)) == null

        cleanup:
        deleteDirect(app)
    }

    /**
     * This test is meant to test the use of the subtree delete control. This needs the configuration feature.use.subtree.delete.control.for.subtree.deletion.enabled
     * to be set to true. By default, it is false. The test needs to be updated to set it to true.
     */
    @Ignore("Ignore this test until feature.use.subtree.delete.control.for.subtree.deletion.enabled is supported in enabled state")
    def "deleteObject(Obj) - verify can delete single object with no subentries using subtree delete control"() {
        setup:
        assert genericApplicationRepository.useSubtreeDeleteControlForSubtreeDeletion()
        Application app = persistClientDirect()
        assert con.getEntry(app.getUniqueId()) != null

        when:
        genericApplicationRepository.deleteObject(app)

        then:
        con.getEntry(app.getUniqueId()) == null

        cleanup:
        deleteDirect(app)
    }

    /**
     * This test is meant to test the use of the subtree delete control. This needs the configuration feature.use.subtree.delete.control.for.subtree.deletion.enabled
     * to be set to true. By default, it is false. The test needs to be updated to set it to true.
     */
    @Ignore("Ignore this test until feature.use.subtree.delete.control.for.subtree.deletion.enabled is supported in enabled state")
    def "deleteObject(Obj) - verify no-op if delete object that no longer exists using subtree delete control"() {
        setup:
        assert genericApplicationRepository.useSubtreeDeleteControlForSubtreeDeletion()
        Application app = persistClientDirect()
        genericApplicationRepository.deleteObject(app) //delete the object from ldap
        con.getEntry(app.getUniqueId()) == null

        expect:
        genericApplicationRepository.deleteObject(app) //trying to delete the object should not throw an exception

        cleanup:
        deleteDirect(app)
    }

    /**
     * This test is meant to test the use of the subtree delete control. This needs the configuration feature.use.subtree.delete.control.for.subtree.deletion.enabled
     * to be set to true. By default, it is false. The test needs to be updated to set it to true.
     */
    @Ignore("Ignore this test until feature.use.subtree.delete.control.for.subtree.deletion.enabled is supported in enabled state")
    def "deleteObject(Obj) - verify no-op if delete object that never existed using subtree delete control"() {
        setup:
        assert genericApplicationRepository.useSubtreeDeleteControlForSubtreeDeletion()
        Application app = mock(Application.class)
        when(app.getUniqueId()).thenReturn("ou=nonexistant,o=rackspace,dc=rackspace,dc=com")

        expect:
        genericApplicationRepository.deleteObject(app)
    }

    /**
     * This test is meant to test the use of the subtree delete control. This needs the configuration feature.use.subtree.delete.control.for.subtree.deletion.enabled
     * to be set to true. By default, it is false. The test needs to be updated to set it to true.
     */
    @Ignore("Ignore this test until feature.use.subtree.delete.control.for.subtree.deletion.enabled is supported in enabled state")
    def "deleteObject(Obj) - verify deleting entry that has children will delete entire tree using subtree delete control"() {
        setup:
        assert genericApplicationRepository.useSubtreeDeleteControlForSubtreeDeletion()
        Application app = persistClientDirect()
        addTokenContainerDirect(app)
        assert con.getEntry(app.getUniqueId()) != null
        assert con.getEntry(getTokenContainerDNForClient(app)) != null

        when:
        genericApplicationRepository.deleteObject(app)

        then:
        assert con.getEntry(app.getUniqueId()) == null
        assert con.getEntry(getTokenContainerDNForClient(app)) == null

        cleanup:
        deleteDirect(app)
    }


    /* ###############################################################################
                        HELPER METHODS
    ############################################################################### */

    def createClient() {
        Application app = new Application("client$FEATURE_RANDOM", ClientSecret.newInstance("secret"), "name", "clientRCN$FEATURE_RANDOM")
        return app;
    }

    def persistClientDirect() {
        Application app = createClient();
        addDirect(app, LdapRepository.APPLICATIONS_BASE_DN)
        return app
    }

    def addTokenContainerDirect(Application app) {
        List<Attribute> attributes = new ArrayList<Attribute>();
        attributes.add(new Attribute(LdapRepository.ATTR_OBJECT_CLASS, LdapRepository.OBJECTCLASS_RACKSPACE_CONTAINER));
        attributes.add(new Attribute(LdapRepository.ATTR_NAME, TESTTOKENS_CONTAINER_NAME));
        Attribute[] attributeArray = attributes.toArray(new Attribute[0]);
        String dn = getTokenContainerDNForClient(app);
        con.add(dn, attributeArray);
    }

    def getTokenContainerDNForClient(Application app) {
        String dn = new LdapRepository.LdapDnBuilder(app.getUniqueId()).addAttribute(LdapRepository.ATTR_NAME, TESTTOKENS_CONTAINER_NAME).build();
        return dn
    }

    /**
     * This function uses the unboundid sdk directly to add data to ldap and bypass rackspace code
     * @param dn
     * @param obj
     * @return
     */
    def addDirect(Application app, String dn) {
        applicationPersister.add(app, con, dn);
    }

    /**
     * This function uses the unboundid sdk directly to delete data from ldap and bypass all rackspace code
     *
     * @param app - app to be deleted
     * @return
     */
    def deleteDirect(Application app) {
        if (app != null) {
            deleteDirect(app.getUniqueId())
        }
    }

    /**
     * This function uses the unboundid sdk directly to delete data from ldap and bypass all rackspace code
     *
     * @param dn - dn of object to be deleted
     * @return
     */
    def deleteDirect(String dn) {
        DeleteRequest deleteRequest = new DeleteRequest(dn);
        deleteRequest.addControl(new SubtreeDeleteRequestControl(true));
        con.delete(deleteRequest);
    }
}
