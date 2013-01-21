package com.rackspace.idm.api.resource.cloud

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain
import com.rackspace.idm.domain.config.providers.cloudv20.Core20XMLWriter
import com.rackspace.idm.exception.BadRequestException
import org.openstack.docs.identity.api.v2.User
import spock.lang.Shared
import spock.lang.Specification
import testHelpers.RootServiceTest

class XMLReaderTest extends RootServiceTest {

    @Shared XMLReader reader
    @Shared Core20XMLWriter writer

    def setupSpec() {
        reader = new XMLReader()
        writer = new Core20XMLWriter().with {
            it.corev20NsPrefixMap = Eval.me('["http://docs.openstack.org/identity/api/v2.0":""]')
            return it
        }
    }

    def "can read and write xml user entity"() {
        given:
        def userEntity = v2Factory.createUser()

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writer.writeTo(userEntity, User.class, null, null, null, null, arrayOutputStream)
        def xml = arrayOutputStream.toString()
        ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(xml.getBytes())
        def readUser = reader.readFrom(null, User.class, null, null, null, arrayInputStream)

        then:
        userEntity.id == readUser.id
        userEntity.username == readUser.username
        userEntity.displayName == readUser.displayName
        userEntity.email == readUser.email
        userEntity.enabled == readUser.enabled
    }

    def "does not allow xml external entity"() {
        given:
        def xml = '<?xml version="1.0" encoding="ISO-8859-1"?> <!DOCTYPE test [ <!ELEMENT test ANY > <!ENTITY xxx SYSTEM "file://///var/log/idm/analytics.log">]> <domain xmlns="http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0" id="domain1b487b2615cf14f358a25c904c3be8b87" name="d    omain1b487b2615cf14f358a25c904c3be8b87" enabled="true"> <description>&xxx;</description> </domain>'

        when:
        ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(xml.getBytes())
        reader.readFrom(null, Domain.class, null, null, null, arrayInputStream)

        then:
        thrown(BadRequestException)
    }
}
