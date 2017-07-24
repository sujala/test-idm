package com.rackspace.idm.api.resource.cloud

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain
import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProviders
import com.rackspace.idm.api.resource.cloud.v20.MultiFactorCloud20Service
import com.rackspace.idm.domain.config.providers.cloudv20.Core20XMLWriter
import com.rackspace.idm.exception.BadRequestException
import org.openstack.docs.identity.api.v2.User
import spock.lang.Shared
import testHelpers.RootServiceTest

class XMLReaderTest extends RootServiceTest {

    @Shared XMLReader reader
    @Shared Core20XMLWriter writer

    def setupSpec() {
        reader = new XMLReader()
        writer = new Core20XMLWriter().with {
            it.corev20NsPrefixMap = Eval.me('["http://docs.openstack.org/identity/api/v2.0":""]')
            it.multiFactorCloud20Service = Mock(MultiFactorCloud20Service)
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

    def "can read identity providers entity"() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<rax-auth:identityProviders xmlns=\"http://docs.openstack.org/identity/api/v2.0\" xmlns:atom=\"http://www.w3.org/2005/Atom\" xmlns:ns4=\"http://docs.rackspace.com/identity/api/ext/RAX-KSGRP/v1.0\" xmlns:os-ksadm=\"http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0\" xmlns:os-ksec2=\"http://docs.openstack.org/identity/api/ext/OS-KSEC2/v1.0\" xmlns:rax-auth=\"http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0\" xmlns:rax-kskey=\"http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0\" xmlns:rax-ksqa=\"http://docs.rackspace.com/identity/api/ext/RAX-KSQA/v1.0\">\n" +
                "\t<rax-auth:identityProvider approvedDomainGroup=\"GLOBAL\" description=\"Identity provider for dedicated\" federationType=\"DOMAIN\" id=\"dedicated\" issuer=\"http://my.rackspace.com\"/>\n" +
                "\t<rax-auth:identityProvider description=\"blah\" federationType=\"RACKER\" id=\"9ffce6860b5b411a9d97ca630def3062\" issuer=\"893e610cfe5e4928ae01adcf1cffd456\"/>\n" +
                "\t<rax-auth:identityProvider description=\"A description\" federationType=\"DOMAIN\" id=\"a5b055835def48b097f6daafe8a1d236\" issuer=\"https://my.issuer.com\">\n" +
                "\t\t<rax-auth:approvedDomainIds>\n" +
                "\t\t\t<rax-auth:approvedDomainId>983452</rax-auth:approvedDomainId>\n" +
                "\t\t</rax-auth:approvedDomainIds>\n" +
                "\t</rax-auth:identityProvider>\n" +
                "</rax-auth:identityProviders>"

        when:
        IdentityProviders identityProviders = reader.readFrom(null, IdentityProviders.class, null, null, null, new ByteArrayInputStream(xml.getBytes()))

        then:
        identityProviders != null
        identityProviders.getIdentityProvider().get(2).getApprovedDomainIds().getApprovedDomainId() != null
        identityProviders.getIdentityProvider().get(2).getApprovedDomainIds().getApprovedDomainId().get(0) == "983452"
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
