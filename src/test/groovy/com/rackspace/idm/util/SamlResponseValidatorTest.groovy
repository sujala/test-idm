package com.rackspace.idm.util
import com.rackspace.idm.domain.dao.ApplicationRoleDao
import com.rackspace.idm.domain.dao.DomainDao
import com.rackspace.idm.domain.dao.IdentityProviderDao
import com.rackspace.idm.domain.decorator.SamlResponseDecorator
import com.rackspace.idm.domain.entity.ClientRole
import com.rackspace.idm.domain.entity.Domain
import com.rackspace.idm.domain.entity.IdentityProvider
import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.validation.PrecedenceValidator
import org.apache.commons.configuration.Configuration
import org.joda.time.DateTime
import org.opensaml.saml2.core.Response
import spock.lang.Shared
import spock.lang.Specification

class SamlResponseValidatorTest extends Specification {
    @Shared SamlResponseValidator samlResponseValidator

    @Shared def IDP_NAME = "dedicated";
    @Shared def IDP_URI = "http://my.test.idp"
    @Shared def IDP_PUBLIC_CERTIFICATE = "--BEGIN CERTIFICATE-- bla bla bla --END CERTIFICATE--"
    @Shared def ROLE_NAME = "observer"
    @Shared def DOMAIN = "1234"

    @Shared def samlUnmarshaller
    @Shared def samlStr
    @Shared Response samlResponse
    @Shared def samlResponseDecorator

    @Shared IdentityProviderDao mockIdentityProviderDao
    @Shared def mockRoleDao
    @Shared def mockSamlSignatureValidator
    @Shared def mockDomainDao
    @Shared def mockPrecedenceValidator
    @Shared def mockConfig

    def setupSpec() {
        //initializes open saml. allows us use unmarshaller
        org.opensaml.DefaultBootstrap.bootstrap();

        samlUnmarshaller = new SamlUnmarshaller()
        samlResponseValidator = new SamlResponseValidator()
    }

    def setup() {
        mockIdentityProviderDao(samlResponseValidator)
        mockRoleDao(samlResponseValidator)
        mockSamlSignatureValidator(samlResponseValidator)
        mockDomainDao(samlResponseValidator)
        mockPrecedenceValidator(samlResponseValidator)
        mockConfig(samlResponseValidator)

        samlStr = "<saml2p:Response xmlns:saml2p=\"urn:oasis:names:tc:SAML:2.0:protocol\" ID=\"bc1c335f-8078-4769-81a1-bb519194279c\" IssueInstant=\"2013-10-01T15:02:42.110Z\" Version=\"2.0\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
                "   <saml2:Issuer xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\">" + IDP_URI + "</saml2:Issuer>\n" +
                "   <ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
                "      <ds:SignedInfo>\n" +
                "         <ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/>\n" +
                "         <ds:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\"/>\n" +
                "         <ds:Reference URI=\"#bc1c335f-8078-4769-81a1-bb519194279c\">\n" +
                "            <ds:Transforms>\n" +
                "               <ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"/>\n" +
                "               <ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\">\n" +
                "                  <ec:InclusiveNamespaces xmlns:ec=\"http://www.w3.org/2001/10/xml-exc-c14n#\" PrefixList=\"xs\"/>\n" +
                "               </ds:Transform>\n" +
                "            </ds:Transforms>\n" +
                "            <ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/>\n" +
                "            <ds:DigestValue>MNSPZoA7K27Mv6oIfePTrMS+W4Y=</ds:DigestValue>\n" +
                "         </ds:Reference>\n" +
                "      </ds:SignedInfo>\n" +
                "      <ds:SignatureValue>LmBStQQ5Xzh/Irlk4/6y123e6xTgvK1xvygCku4qpKoIEgd5vjTVkH7q6ol49Fqe1DcfJ6tYTrmAq9UL+7meGg==</ds:SignatureValue>\n" +
                "   </ds:Signature>\n" +
                "   <saml2p:Status>\n" +
                "      <saml2p:StatusCode Value=\"urn:oasis:names:tc:SAML:2.0:status:Success\"/>\n" +
                "   </saml2p:Status>\n" +
                "   <saml2:Assertion xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\" ID=\"461e5f27-3880-4ef3-a51f-8265ef13b7e6\" IssueInstant=\"2013-10-01T15:02:42.107Z\" Version=\"2.0\">\n" +
                "      <saml2:Issuer>" + IDP_URI + "</saml2:Issuer>\n" +
                "      <saml2:Subject>\n" +
                "         <saml2:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified\">john.doe</saml2:NameID>\n" +
                "         <saml2:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\">\n" +
                "            <saml2:SubjectConfirmationData NotOnOrAfter=\"2020-10-01T17:02:42.101Z\"/>\n" +
                "         </saml2:SubjectConfirmation>\n" +
                "      </saml2:Subject>\n" +
                "      <saml2:AuthnStatement AuthnInstant=\"2013-10-01T15:02:42.103Z\">\n" +
                "         <saml2:AuthnContext>\n" +
                "            <saml2:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordPotectedTransport</saml2:AuthnContextClassRef>\n" +
                "         </saml2:AuthnContext>\n" +
                "      </saml2:AuthnStatement>\n" +
                "      <saml2:AttributeStatement>\n" +
                "         <saml2:Attribute Name=\"roles\">\n" +
                "            <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">" + ROLE_NAME + "</saml2:AttributeValue>\n" +
                "         </saml2:Attribute>\n" +
                "         <saml2:Attribute Name=\"domain\">\n" +
                "            <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">" + DOMAIN + "</saml2:AttributeValue>\n" +
                "         </saml2:Attribute>\n" +
                "      </saml2:AttributeStatement>\n" +
                "   </saml2:Assertion>\n" +
                "</saml2p:Response>"
    }

    def "validate correct saml response" (){
        given:
        samlResponse = samlUnmarshaller.unmarshallResponse(samlStr)
        samlResponseDecorator = new SamlResponseDecorator(samlResponse)
        def mockUserAdminRole = Mock(ClientRole)

        and:
        mockIdentityProviderDao.getIdentityProviderByUri(IDP_URI) >> createIdentityProvider()
        mockRoleDao.getRoleByName(ROLE_NAME) >> Mock(ClientRole)
        mockDomainDao.getDomain(DOMAIN) >> Mock(Domain)
        mockConfig.getString("cloudAuth.userAdminRole") >> "identity:user-admin"
        mockRoleDao.getRoleByName("identity:user-admin") >> mockUserAdminRole

        when:
        samlResponseValidator.validate(samlResponseDecorator)

        then:
        noExceptionThrown()
        1 * mockSamlSignatureValidator.validateSignature(_,IDP_PUBLIC_CERTIFICATE)
        1 * mockPrecedenceValidator.verifyRolePrecedenceForAssignment(mockUserAdminRole, _);
    }

    def "validate saml response when signature is not specified" (){
        given:
        samlResponse = samlUnmarshaller.unmarshallResponse(samlStr)
        samlResponse.setSignature(null)
        samlResponseDecorator = new SamlResponseDecorator(samlResponse)

        and:
        mockIdentityProviderDao.getIdentityProviderByUri(IDP_URI) >> createIdentityProvider()

        when:
        samlResponseValidator.validate(samlResponseDecorator)

        then:
        thrown(BadRequestException)
    }

    def "validate saml response when signature is invalid" (){
        given:
        samlResponse = samlUnmarshaller.unmarshallResponse(samlStr)
        samlResponseDecorator = new SamlResponseDecorator(samlResponse)

        and:
        mockIdentityProviderDao.getIdentityProviderByUri(IDP_URI) >> createIdentityProvider()
        1 * mockSamlSignatureValidator.validateSignature(_,IDP_PUBLIC_CERTIFICATE) >> { throw new Exception("Invalid Siganture")}

        when:
        samlResponseValidator.validate(samlResponseDecorator)

        then:
        thrown(BadRequestException)
    }

    def "validate saml response when issuer is not specified" (){
        samlResponse = samlUnmarshaller.unmarshallResponse(samlStr)
        samlResponse.setIssuer(null)
        samlResponse.getAssertions().get(0).setIssuer(null)
        samlResponseDecorator = new SamlResponseDecorator(samlResponse)

        when:
        samlResponseValidator.validate(samlResponseDecorator)

        then:
        thrown(BadRequestException)
    }

    def "validate saml response when incorrect issuer is specified" (){
        given:
        samlResponse = samlUnmarshaller.unmarshallResponse(samlStr)
        samlResponseDecorator = new SamlResponseDecorator(samlResponse)

        and:
        mockIdentityProviderDao.getIdentityProviderByUri(IDP_URI) >> null

        when:
        samlResponseValidator.validate(samlResponseDecorator)

        then:
        thrown(BadRequestException)
    }

    def "validate saml response when subject is not specified" (){
        given:
        samlResponse = samlUnmarshaller.unmarshallResponse(samlStr)
        samlResponse.getAssertions().get(0).getSubject().setNameID(null)
        samlResponseDecorator = new SamlResponseDecorator(samlResponse)

        and:
        mockIdentityProviderDao.getIdentityProviderByUri(IDP_URI) >> createIdentityProvider()

        when:
        samlResponseValidator.validate(samlResponseDecorator)

        then:
        thrown(BadRequestException)
    }

    def "validate saml response when subject confirmation NotOnOrAfter is not specified" (){
        given:
        samlResponse = samlUnmarshaller.unmarshallResponse(samlStr)
        samlResponse.getAssertions().get(0).getSubject().getSubjectConfirmations().get(0).getSubjectConfirmationData().setNotOnOrAfter(null)
        samlResponseDecorator = new SamlResponseDecorator(samlResponse)

        and:
        mockIdentityProviderDao.getIdentityProviderByUri(IDP_URI) >> createIdentityProvider()

        when:
        samlResponseValidator.validate(samlResponseDecorator)

        then:
        thrown(BadRequestException)
    }

    def "validate saml response when subject confirmation NotOnOrAfter is in the past" (){
        given:
        samlResponse = samlUnmarshaller.unmarshallResponse(samlStr)
        samlResponse.getAssertions().get(0).getSubject().getSubjectConfirmations().get(0).getSubjectConfirmationData().setNotOnOrAfter(new DateTime().minusDays(2))
        samlResponseDecorator = new SamlResponseDecorator(samlResponse)

        and:
        mockIdentityProviderDao.getIdentityProviderByUri(IDP_URI) >> createIdentityProvider()

        when:
        samlResponseValidator.validate(samlResponseDecorator)

        then:
        thrown(BadRequestException)
    }

    def "validate saml response when AuthnInstant is not specified" (){
        given:
        samlResponse = samlUnmarshaller.unmarshallResponse(samlStr)
        samlResponse.getAssertions().get(0).getAuthnStatements().get(0).setAuthnInstant(null)
        samlResponseDecorator = new SamlResponseDecorator(samlResponse)

        and:
        mockIdentityProviderDao.getIdentityProviderByUri(IDP_URI) >> createIdentityProvider()

        when:
        samlResponseValidator.validate(samlResponseDecorator)

        then:
        thrown(BadRequestException)
    }

    def "validate saml response when AuthContextClassRef is not specified" (){
        given:
        samlResponse = samlUnmarshaller.unmarshallResponse(samlStr)
        samlResponse.getAssertions().get(0).getAuthnStatements().get(0).getAuthnContext().setAuthnContextClassRef(null)
        samlResponseDecorator = new SamlResponseDecorator(samlResponse)

        and:
        mockIdentityProviderDao.getIdentityProviderByUri(IDP_URI) >> createIdentityProvider()

        when:
        samlResponseValidator.validate(samlResponseDecorator)

        then:
        thrown(BadRequestException)
    }

    def "validate saml response when AuthContextClassRef is invalid" (){
        given:
        samlResponse = samlUnmarshaller.unmarshallResponse(samlStr)
        samlResponse.getAssertions().get(0).getAuthnStatements().get(0).getAuthnContext().getAuthnContextClassRef().setAuthnContextClassRef("fake String");

        samlResponseDecorator = new SamlResponseDecorator(samlResponse)

        and:
        mockIdentityProviderDao.getIdentityProviderByUri(IDP_URI) >> createIdentityProvider()

        when:
        samlResponseValidator.validate(samlResponseDecorator)

        then:
        thrown(BadRequestException)
    }

    def "validate saml response when domain attribute is not specified" (){
        given:

        samlStr = "<saml2p:Response xmlns:saml2p=\"urn:oasis:names:tc:SAML:2.0:protocol\" ID=\"bc1c335f-8078-4769-81a1-bb519194279c\" IssueInstant=\"2013-10-01T15:02:42.110Z\" Version=\"2.0\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
                "   <saml2:Issuer xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\">" + IDP_URI + "</saml2:Issuer>\n" +
                "   <ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
                "      <ds:SignedInfo>\n" +
                "         <ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/>\n" +
                "         <ds:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\"/>\n" +
                "         <ds:Reference URI=\"#bc1c335f-8078-4769-81a1-bb519194279c\">\n" +
                "            <ds:Transforms>\n" +
                "               <ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"/>\n" +
                "               <ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\">\n" +
                "                  <ec:InclusiveNamespaces xmlns:ec=\"http://www.w3.org/2001/10/xml-exc-c14n#\" PrefixList=\"xs\"/>\n" +
                "               </ds:Transform>\n" +
                "            </ds:Transforms>\n" +
                "            <ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/>\n" +
                "            <ds:DigestValue>MNSPZoA7K27Mv6oIfePTrMS+W4Y=</ds:DigestValue>\n" +
                "         </ds:Reference>\n" +
                "      </ds:SignedInfo>\n" +
                "      <ds:SignatureValue>LmBStQQ5Xzh/Irlk4/6y123e6xTgvK1xvygCku4qpKoIEgd5vjTVkH7q6ol49Fqe1DcfJ6tYTrmAq9UL+7meGg==</ds:SignatureValue>\n" +
                "   </ds:Signature>\n" +
                "   <saml2p:Status>\n" +
                "      <saml2p:StatusCode Value=\"urn:oasis:names:tc:SAML:2.0:status:Success\"/>\n" +
                "   </saml2p:Status>\n" +
                "   <saml2:Assertion xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\" ID=\"461e5f27-3880-4ef3-a51f-8265ef13b7e6\" IssueInstant=\"2013-10-01T15:02:42.107Z\" Version=\"2.0\">\n" +
                "      <saml2:Issuer>" + IDP_URI + "</saml2:Issuer>\n" +
                "      <saml2:Subject>\n" +
                "         <saml2:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified\">john.doe</saml2:NameID>\n" +
                "         <saml2:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\">\n" +
                "            <saml2:SubjectConfirmationData NotOnOrAfter=\"2020-10-01T17:02:42.101Z\"/>\n" +
                "         </saml2:SubjectConfirmation>\n" +
                "      </saml2:Subject>\n" +
                "      <saml2:AuthnStatement AuthnInstant=\"2013-10-01T15:02:42.103Z\">\n" +
                "         <saml2:AuthnContext>\n" +
                "            <saml2:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordPotectedTransport</saml2:AuthnContextClassRef>\n" +
                "         </saml2:AuthnContext>\n" +
                "      </saml2:AuthnStatement>\n" +
                "      <saml2:AttributeStatement>\n" +
                "         <saml2:Attribute Name=\"roles\">\n" +
                "            <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">" + ROLE_NAME + "</saml2:AttributeValue>\n" +
                "         </saml2:Attribute>\n" +
                "      </saml2:AttributeStatement>\n" +
                "   </saml2:Assertion>\n" +
                "</saml2p:Response>"

        samlResponse = samlUnmarshaller.unmarshallResponse(samlStr)
        samlResponseDecorator = new SamlResponseDecorator(samlResponse)

        and:
        mockIdentityProviderDao.getIdentityProviderByUri(IDP_URI) >> createIdentityProvider()
        mockRoleDao.getRoleByName(ROLE_NAME) >> Mock(ClientRole)

        when:
        samlResponseValidator.validate(samlResponseDecorator)

        then:
        thrown(BadRequestException)
    }

    def "validate saml response when domain attribute is specified more than once" (){
        given:

        samlStr = "<saml2p:Response xmlns:saml2p=\"urn:oasis:names:tc:SAML:2.0:protocol\" ID=\"bc1c335f-8078-4769-81a1-bb519194279c\" IssueInstant=\"2013-10-01T15:02:42.110Z\" Version=\"2.0\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
                "   <saml2:Issuer xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\">" + IDP_URI + "</saml2:Issuer>\n" +
                "   <ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
                "      <ds:SignedInfo>\n" +
                "         <ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/>\n" +
                "         <ds:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\"/>\n" +
                "         <ds:Reference URI=\"#bc1c335f-8078-4769-81a1-bb519194279c\">\n" +
                "            <ds:Transforms>\n" +
                "               <ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"/>\n" +
                "               <ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\">\n" +
                "                  <ec:InclusiveNamespaces xmlns:ec=\"http://www.w3.org/2001/10/xml-exc-c14n#\" PrefixList=\"xs\"/>\n" +
                "               </ds:Transform>\n" +
                "            </ds:Transforms>\n" +
                "            <ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/>\n" +
                "            <ds:DigestValue>MNSPZoA7K27Mv6oIfePTrMS+W4Y=</ds:DigestValue>\n" +
                "         </ds:Reference>\n" +
                "      </ds:SignedInfo>\n" +
                "      <ds:SignatureValue>LmBStQQ5Xzh/Irlk4/6y123e6xTgvK1xvygCku4qpKoIEgd5vjTVkH7q6ol49Fqe1DcfJ6tYTrmAq9UL+7meGg==</ds:SignatureValue>\n" +
                "   </ds:Signature>\n" +
                "   <saml2p:Status>\n" +
                "      <saml2p:StatusCode Value=\"urn:oasis:names:tc:SAML:2.0:status:Success\"/>\n" +
                "   </saml2p:Status>\n" +
                "   <saml2:Assertion xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\" ID=\"461e5f27-3880-4ef3-a51f-8265ef13b7e6\" IssueInstant=\"2013-10-01T15:02:42.107Z\" Version=\"2.0\">\n" +
                "      <saml2:Issuer>" + IDP_URI + "</saml2:Issuer>\n" +
                "      <saml2:Subject>\n" +
                "         <saml2:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified\">john.doe</saml2:NameID>\n" +
                "         <saml2:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\">\n" +
                "            <saml2:SubjectConfirmationData NotOnOrAfter=\"2020-10-01T17:02:42.101Z\"/>\n" +
                "         </saml2:SubjectConfirmation>\n" +
                "      </saml2:Subject>\n" +
                "      <saml2:AuthnStatement AuthnInstant=\"2013-10-01T15:02:42.103Z\">\n" +
                "         <saml2:AuthnContext>\n" +
                "            <saml2:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordPotectedTransport</saml2:AuthnContextClassRef>\n" +
                "         </saml2:AuthnContext>\n" +
                "      </saml2:AuthnStatement>\n" +
                "      <saml2:AttributeStatement>\n" +
                "         <saml2:Attribute Name=\"roles\">\n" +
                "            <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">" + ROLE_NAME + "</saml2:AttributeValue>\n" +
                "         </saml2:Attribute>\n" +
                "         <saml2:Attribute Name=\"domain\">\n" +
                "            <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">1234</saml2:AttributeValue>\n" +
                "            <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">5678</saml2:AttributeValue>\n" +
                "         </saml2:Attribute>\n" +
                "      </saml2:AttributeStatement>\n" +
                "   </saml2:Assertion>\n" +
                "</saml2p:Response>"

        samlResponse = samlUnmarshaller.unmarshallResponse(samlStr)
        samlResponseDecorator = new SamlResponseDecorator(samlResponse)

        and:
        mockIdentityProviderDao.getIdentityProviderByUri(IDP_URI) >> createIdentityProvider()
        mockRoleDao.getRoleByName(ROLE_NAME) >> Mock(ClientRole)

        when:
        samlResponseValidator.validate(samlResponseDecorator)

        then:
        thrown(BadRequestException)
    }

    def "validate saml response when domain does not exist" (){
        given:

        samlStr = "<saml2p:Response xmlns:saml2p=\"urn:oasis:names:tc:SAML:2.0:protocol\" ID=\"bc1c335f-8078-4769-81a1-bb519194279c\" IssueInstant=\"2013-10-01T15:02:42.110Z\" Version=\"2.0\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
                "   <saml2:Issuer xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\">" + IDP_URI + "</saml2:Issuer>\n" +
                "   <ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
                "      <ds:SignedInfo>\n" +
                "         <ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/>\n" +
                "         <ds:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\"/>\n" +
                "         <ds:Reference URI=\"#bc1c335f-8078-4769-81a1-bb519194279c\">\n" +
                "            <ds:Transforms>\n" +
                "               <ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"/>\n" +
                "               <ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\">\n" +
                "                  <ec:InclusiveNamespaces xmlns:ec=\"http://www.w3.org/2001/10/xml-exc-c14n#\" PrefixList=\"xs\"/>\n" +
                "               </ds:Transform>\n" +
                "            </ds:Transforms>\n" +
                "            <ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/>\n" +
                "            <ds:DigestValue>MNSPZoA7K27Mv6oIfePTrMS+W4Y=</ds:DigestValue>\n" +
                "         </ds:Reference>\n" +
                "      </ds:SignedInfo>\n" +
                "      <ds:SignatureValue>LmBStQQ5Xzh/Irlk4/6y123e6xTgvK1xvygCku4qpKoIEgd5vjTVkH7q6ol49Fqe1DcfJ6tYTrmAq9UL+7meGg==</ds:SignatureValue>\n" +
                "   </ds:Signature>\n" +
                "   <saml2p:Status>\n" +
                "      <saml2p:StatusCode Value=\"urn:oasis:names:tc:SAML:2.0:status:Success\"/>\n" +
                "   </saml2p:Status>\n" +
                "   <saml2:Assertion xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\" ID=\"461e5f27-3880-4ef3-a51f-8265ef13b7e6\" IssueInstant=\"2013-10-01T15:02:42.107Z\" Version=\"2.0\">\n" +
                "      <saml2:Issuer>" + IDP_URI + "</saml2:Issuer>\n" +
                "      <saml2:Subject>\n" +
                "         <saml2:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified\">john.doe</saml2:NameID>\n" +
                "         <saml2:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\">\n" +
                "            <saml2:SubjectConfirmationData NotOnOrAfter=\"2020-10-01T17:02:42.101Z\"/>\n" +
                "         </saml2:SubjectConfirmation>\n" +
                "      </saml2:Subject>\n" +
                "      <saml2:AuthnStatement AuthnInstant=\"2013-10-01T15:02:42.103Z\">\n" +
                "         <saml2:AuthnContext>\n" +
                "            <saml2:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordPotectedTransport</saml2:AuthnContextClassRef>\n" +
                "         </saml2:AuthnContext>\n" +
                "      </saml2:AuthnStatement>\n" +
                "      <saml2:AttributeStatement>\n" +
                "         <saml2:Attribute Name=\"roles\">\n" +
                "            <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">" + ROLE_NAME + "</saml2:AttributeValue>\n" +
                "         </saml2:Attribute>\n" +
                "         <saml2:Attribute Name=\"domain\">\n" +
                "            <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">1234</saml2:AttributeValue>\n" +
                "         </saml2:Attribute>\n" +
                "      </saml2:AttributeStatement>\n" +
                "   </saml2:Assertion>\n" +
                "</saml2p:Response>"

        samlResponse = samlUnmarshaller.unmarshallResponse(samlStr)
        samlResponseDecorator = new SamlResponseDecorator(samlResponse)

        and:
        mockIdentityProviderDao.getIdentityProviderByUri(IDP_URI) >> createIdentityProvider()
        mockRoleDao.getRoleByName(ROLE_NAME) >> Mock(ClientRole)

        when:
        samlResponseValidator.validate(samlResponseDecorator)

        then:
        thrown(BadRequestException)
    }

    def "validate saml response when roles attribute is not specified" (){
        given:

        samlStr = "<saml2p:Response xmlns:saml2p=\"urn:oasis:names:tc:SAML:2.0:protocol\" ID=\"bc1c335f-8078-4769-81a1-bb519194279c\" IssueInstant=\"2013-10-01T15:02:42.110Z\" Version=\"2.0\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
                "   <saml2:Issuer xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\">" + IDP_URI + "</saml2:Issuer>\n" +
                "   <ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
                "      <ds:SignedInfo>\n" +
                "         <ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/>\n" +
                "         <ds:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\"/>\n" +
                "         <ds:Reference URI=\"#bc1c335f-8078-4769-81a1-bb519194279c\">\n" +
                "            <ds:Transforms>\n" +
                "               <ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"/>\n" +
                "               <ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\">\n" +
                "                  <ec:InclusiveNamespaces xmlns:ec=\"http://www.w3.org/2001/10/xml-exc-c14n#\" PrefixList=\"xs\"/>\n" +
                "               </ds:Transform>\n" +
                "            </ds:Transforms>\n" +
                "            <ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/>\n" +
                "            <ds:DigestValue>MNSPZoA7K27Mv6oIfePTrMS+W4Y=</ds:DigestValue>\n" +
                "         </ds:Reference>\n" +
                "      </ds:SignedInfo>\n" +
                "      <ds:SignatureValue>LmBStQQ5Xzh/Irlk4/6y123e6xTgvK1xvygCku4qpKoIEgd5vjTVkH7q6ol49Fqe1DcfJ6tYTrmAq9UL+7meGg==</ds:SignatureValue>\n" +
                "   </ds:Signature>\n" +
                "   <saml2p:Status>\n" +
                "      <saml2p:StatusCode Value=\"urn:oasis:names:tc:SAML:2.0:status:Success\"/>\n" +
                "   </saml2p:Status>\n" +
                "   <saml2:Assertion xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\" ID=\"461e5f27-3880-4ef3-a51f-8265ef13b7e6\" IssueInstant=\"2013-10-01T15:02:42.107Z\" Version=\"2.0\">\n" +
                "      <saml2:Issuer>" + IDP_URI + "</saml2:Issuer>\n" +
                "      <saml2:Subject>\n" +
                "         <saml2:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified\">john.doe</saml2:NameID>\n" +
                "         <saml2:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\">\n" +
                "            <saml2:SubjectConfirmationData NotOnOrAfter=\"2020-10-01T17:02:42.101Z\"/>\n" +
                "         </saml2:SubjectConfirmation>\n" +
                "      </saml2:Subject>\n" +
                "      <saml2:AuthnStatement AuthnInstant=\"2013-10-01T15:02:42.103Z\">\n" +
                "         <saml2:AuthnContext>\n" +
                "            <saml2:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordPotectedTransport</saml2:AuthnContextClassRef>\n" +
                "         </saml2:AuthnContext>\n" +
                "      </saml2:AuthnStatement>\n" +
                "      <saml2:AttributeStatement>\n" +
                "         <saml2:Attribute Name=\"domain\">\n" +
                "            <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">1234</saml2:AttributeValue>\n" +
                "         </saml2:Attribute>\n" +
                "      </saml2:AttributeStatement>\n" +
                "   </saml2:Assertion>\n" +
                "</saml2p:Response>"

        samlResponse = samlUnmarshaller.unmarshallResponse(samlStr)
        samlResponseDecorator = new SamlResponseDecorator(samlResponse)

        and:
        mockIdentityProviderDao.getIdentityProviderByUri(IDP_URI) >> createIdentityProvider()

        when:
        samlResponseValidator.validate(samlResponseDecorator)

        then:
        thrown(BadRequestException)
    }

    def "validate saml response when role does not exist" (){
        given:

        samlStr = "<saml2p:Response xmlns:saml2p=\"urn:oasis:names:tc:SAML:2.0:protocol\" ID=\"bc1c335f-8078-4769-81a1-bb519194279c\" IssueInstant=\"2013-10-01T15:02:42.110Z\" Version=\"2.0\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
                "   <saml2:Issuer xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\">" + IDP_URI + "</saml2:Issuer>\n" +
                "   <ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
                "      <ds:SignedInfo>\n" +
                "         <ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/>\n" +
                "         <ds:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\"/>\n" +
                "         <ds:Reference URI=\"#bc1c335f-8078-4769-81a1-bb519194279c\">\n" +
                "            <ds:Transforms>\n" +
                "               <ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"/>\n" +
                "               <ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\">\n" +
                "                  <ec:InclusiveNamespaces xmlns:ec=\"http://www.w3.org/2001/10/xml-exc-c14n#\" PrefixList=\"xs\"/>\n" +
                "               </ds:Transform>\n" +
                "            </ds:Transforms>\n" +
                "            <ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/>\n" +
                "            <ds:DigestValue>MNSPZoA7K27Mv6oIfePTrMS+W4Y=</ds:DigestValue>\n" +
                "         </ds:Reference>\n" +
                "      </ds:SignedInfo>\n" +
                "      <ds:SignatureValue>LmBStQQ5Xzh/Irlk4/6y123e6xTgvK1xvygCku4qpKoIEgd5vjTVkH7q6ol49Fqe1DcfJ6tYTrmAq9UL+7meGg==</ds:SignatureValue>\n" +
                "   </ds:Signature>\n" +
                "   <saml2p:Status>\n" +
                "      <saml2p:StatusCode Value=\"urn:oasis:names:tc:SAML:2.0:status:Success\"/>\n" +
                "   </saml2p:Status>\n" +
                "   <saml2:Assertion xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\" ID=\"461e5f27-3880-4ef3-a51f-8265ef13b7e6\" IssueInstant=\"2013-10-01T15:02:42.107Z\" Version=\"2.0\">\n" +
                "      <saml2:Issuer>" + IDP_URI + "</saml2:Issuer>\n" +
                "      <saml2:Subject>\n" +
                "         <saml2:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified\">john.doe</saml2:NameID>\n" +
                "         <saml2:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\">\n" +
                "            <saml2:SubjectConfirmationData NotOnOrAfter=\"2020-10-01T17:02:42.101Z\"/>\n" +
                "         </saml2:SubjectConfirmation>\n" +
                "      </saml2:Subject>\n" +
                "      <saml2:AuthnStatement AuthnInstant=\"2013-10-01T15:02:42.103Z\">\n" +
                "         <saml2:AuthnContext>\n" +
                "            <saml2:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordPotectedTransport</saml2:AuthnContextClassRef>\n" +
                "         </saml2:AuthnContext>\n" +
                "      </saml2:AuthnStatement>\n" +
                "      <saml2:AttributeStatement>\n" +
                "         <saml2:Attribute Name=\"domain\">\n" +
                "            <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">1234</saml2:AttributeValue>\n" +
                "         </saml2:Attribute>\n" +
                "         <saml2:Attribute Name=\"roles\">\n" +
                "            <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">" + ROLE_NAME + "</saml2:AttributeValue>\n" +
                "         </saml2:Attribute>\n" +
                "      </saml2:AttributeStatement>\n" +
                "   </saml2:Assertion>\n" +
                "</saml2p:Response>"

        samlResponse = samlUnmarshaller.unmarshallResponse(samlStr)
        samlResponseDecorator = new SamlResponseDecorator(samlResponse)

        and:
        mockIdentityProviderDao.getIdentityProviderByUri(IDP_URI) >> createIdentityProvider()
        mockRoleDao.getRoleByName(ROLE_NAME) >> null

        when:
        samlResponseValidator.validate(samlResponseDecorator)

        then:
        thrown(BadRequestException)
    }

    def "validate saml response when same role specified more than once" (){
        given:

        samlStr = "<saml2p:Response xmlns:saml2p=\"urn:oasis:names:tc:SAML:2.0:protocol\" ID=\"bc1c335f-8078-4769-81a1-bb519194279c\" IssueInstant=\"2013-10-01T15:02:42.110Z\" Version=\"2.0\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
                "   <saml2:Issuer xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\">" + IDP_URI + "</saml2:Issuer>\n" +
                "   <ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
                "      <ds:SignedInfo>\n" +
                "         <ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/>\n" +
                "         <ds:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\"/>\n" +
                "         <ds:Reference URI=\"#bc1c335f-8078-4769-81a1-bb519194279c\">\n" +
                "            <ds:Transforms>\n" +
                "               <ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"/>\n" +
                "               <ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\">\n" +
                "                  <ec:InclusiveNamespaces xmlns:ec=\"http://www.w3.org/2001/10/xml-exc-c14n#\" PrefixList=\"xs\"/>\n" +
                "               </ds:Transform>\n" +
                "            </ds:Transforms>\n" +
                "            <ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/>\n" +
                "            <ds:DigestValue>MNSPZoA7K27Mv6oIfePTrMS+W4Y=</ds:DigestValue>\n" +
                "         </ds:Reference>\n" +
                "      </ds:SignedInfo>\n" +
                "      <ds:SignatureValue>LmBStQQ5Xzh/Irlk4/6y123e6xTgvK1xvygCku4qpKoIEgd5vjTVkH7q6ol49Fqe1DcfJ6tYTrmAq9UL+7meGg==</ds:SignatureValue>\n" +
                "   </ds:Signature>\n" +
                "   <saml2p:Status>\n" +
                "      <saml2p:StatusCode Value=\"urn:oasis:names:tc:SAML:2.0:status:Success\"/>\n" +
                "   </saml2p:Status>\n" +
                "   <saml2:Assertion xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\" ID=\"461e5f27-3880-4ef3-a51f-8265ef13b7e6\" IssueInstant=\"2013-10-01T15:02:42.107Z\" Version=\"2.0\">\n" +
                "      <saml2:Issuer>" + IDP_URI + "</saml2:Issuer>\n" +
                "      <saml2:Subject>\n" +
                "         <saml2:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified\">john.doe</saml2:NameID>\n" +
                "         <saml2:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\">\n" +
                "            <saml2:SubjectConfirmationData NotOnOrAfter=\"2020-10-01T17:02:42.101Z\"/>\n" +
                "         </saml2:SubjectConfirmation>\n" +
                "      </saml2:Subject>\n" +
                "      <saml2:AuthnStatement AuthnInstant=\"2013-10-01T15:02:42.103Z\">\n" +
                "         <saml2:AuthnContext>\n" +
                "            <saml2:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordPotectedTransport</saml2:AuthnContextClassRef>\n" +
                "         </saml2:AuthnContext>\n" +
                "      </saml2:AuthnStatement>\n" +
                "      <saml2:AttributeStatement>\n" +
                "         <saml2:Attribute Name=\"domain\">\n" +
                "            <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">1234</saml2:AttributeValue>\n" +
                "         </saml2:Attribute>\n" +
                "         <saml2:Attribute Name=\"roles\">\n" +
                "            <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">" + ROLE_NAME + "</saml2:AttributeValue>\n" +
                "            <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">" + ROLE_NAME + "</saml2:AttributeValue>\n" +
                "         </saml2:Attribute>\n" +
                "      </saml2:AttributeStatement>\n" +
                "   </saml2:Assertion>\n" +
                "</saml2p:Response>"

        samlResponse = samlUnmarshaller.unmarshallResponse(samlStr)
        samlResponseDecorator = new SamlResponseDecorator(samlResponse)

        and:
        mockIdentityProviderDao.getIdentityProviderByUri(IDP_URI) >> createIdentityProvider()
        mockRoleDao.getRoleByName(ROLE_NAME) >> Mock(ClientRole)

        when:
        samlResponseValidator.validate(samlResponseDecorator)

        then:
        thrown(BadRequestException)
    }

    def mockIdentityProviderDao(validator) {
        mockIdentityProviderDao = Mock(IdentityProviderDao)
        validator.identityProviderDao = mockIdentityProviderDao
    }

    def mockRoleDao(validator) {
        mockRoleDao = Mock(ApplicationRoleDao)
        validator.roleDao = mockRoleDao
    }

    def mockDomainDao(validator) {
        mockDomainDao = Mock(DomainDao)
        validator.domainDao = mockDomainDao
    }

    def mockSamlSignatureValidator(validator) {
        mockSamlSignatureValidator = Mock(SamlSignatureValidator)
        validator.samlSignatureValidator = mockSamlSignatureValidator
    }

    def mockPrecedenceValidator(validator) {
        mockPrecedenceValidator = Mock(PrecedenceValidator)
        validator.precedenceValidator = mockPrecedenceValidator
    }

    def mockConfig(validator) {
        mockConfig = Mock(Configuration)
        validator.config = mockConfig
    }

    def createIdentityProvider() {
        new IdentityProvider().with({
            it.name = IDP_NAME
            it.uri = IDP_URI
            it.publicCertificate = IDP_PUBLIC_CERTIFICATE
            return it
        })
    }

}