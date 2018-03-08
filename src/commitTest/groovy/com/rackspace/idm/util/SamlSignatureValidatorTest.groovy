package com.rackspace.idm.util

import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.exception.SignatureValidationException
import org.opensaml.core.config.InitializationService
import spock.lang.Shared
import spock.lang.Specification
import testHelpers.saml.SamlCredentialUtils

import java.security.cert.X509Certificate

class SamlSignatureValidatorTest extends Specification {
    @Shared SamlSignatureValidator samlSignatureValidator
    @Shared SamlUnmarshaller samlUnmarshaller

    @Shared IdentityConfig identityConfig = Mock(IdentityConfig)
    @Shared IdentityConfig.StaticConfig staticConfig = Mock(IdentityConfig.StaticConfig)
    @Shared IdentityConfig.ReloadableConfig reloadableConfig = Mock(IdentityConfig.ReloadableConfig)


    //This certificate was what was used to generate the valid saml string in test below
    @Shared String userCertificateStr = "" +
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIB9TCCAZ+gAwIBAgIJAMpuL8r3peCbMA0GCSqGSIb3DQEBBQUAMFYxCzAJBgNV\n" +
            "BAYTAlVTMQ4wDAYDVQQIDAVUZXhhczEUMBIGA1UEBwwLU2FuIEFudG9uaW8xITAf\n" +
            "BgNVBAoMGEludGVybmV0IFdpZGdpdHMgUHR5IEx0ZDAeFw0xMzA3MDkyMjI3NDla\n" +
            "Fw0xNDA3MDkyMjI3NDlaMFYxCzAJBgNVBAYTAlVTMQ4wDAYDVQQIDAVUZXhhczEU\n" +
            "MBIGA1UEBwwLU2FuIEFudG9uaW8xITAfBgNVBAoMGEludGVybmV0IFdpZGdpdHMg\n" +
            "UHR5IEx0ZDBcMA0GCSqGSIb3DQEBAQUAA0sAMEgCQQC0tL3PylPxPj1auMN2CNWI\n" +
            "aCsqPcy4LK9OukjxSfUkewOi6kKov9aS2pe0AsC8uwkhPj7Pl+w0lVYyGSrAh58P\n" +
            "AgMBAAGjUDBOMB0GA1UdDgQWBBQI8Ezg9M8vnXo7it28nUW6xY974jAfBgNVHSME\n" +
            "GDAWgBQI8Ezg9M8vnXo7it28nUW6xY974jAMBgNVHRMEBTADAQH/MA0GCSqGSIb3\n" +
            "DQEBBQUAA0EAOo8UpA+lFmFn/DX4lEgShYpcom11O+KS3mf8NJhl2/34wLw0IJ0t\n" +
            "1Bp6+UKeELUVXUa8eptaQuJnsj6xMbayxw==\n" +
            "-----END CERTIFICATE-----\n"

    @Shared X509Certificate userCertificate =  SamlCredentialUtils.toX509Certificate(userCertificateStr)

    def samlStr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><saml2p:Response xmlns:saml2p=\"urn:oasis:names:tc:SAML:2.0:protocol\" ID=\"93bfd9e1-8f52-4752-9501-3f5c66bd1329\" IssueInstant=\"2013-10-15T02:47:36.943Z\" Version=\"2.0\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><saml2:Issuer xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\">dedicated</saml2:Issuer><ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"><ds:SignedInfo><ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/><ds:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\"/><ds:Reference URI=\"#93bfd9e1-8f52-4752-9501-3f5c66bd1329\"><ds:Transforms><ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"/><ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"><ec:InclusiveNamespaces xmlns:ec=\"http://www.w3.org/2001/10/xml-exc-c14n#\" PrefixList=\"xs\"/></ds:Transform></ds:Transforms><ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/><ds:DigestValue>Edc+y8y+4bVedEsSDbyTB3Vvb0c=</ds:DigestValue></ds:Reference></ds:SignedInfo><ds:SignatureValue>Z2uj3ThYHrMHkp93DoL18R//ABxZRTHzLJPLLY8rMjTE5L4ABuuDwxULeyQzGEnou5u2/Of/z9+lCvkBZF/WJA==</ds:SignatureValue></ds:Signature><saml2p:Status><saml2p:StatusCode Value=\"urn:oasis:names:tc:SAML:2.0:status:Success\"/></saml2p:Status><saml2:Assertion xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\" ID=\"ab8403a9-335b-4c99-8bca-3341be9b0fcd\" IssueInstant=\"2013-10-15T02:47:36.922Z\" Version=\"2.0\"><saml2:Issuer>dedicated</saml2:Issuer><saml2:Subject><saml2:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified\">john.doe</saml2:NameID><saml2:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\"><saml2:SubjectConfirmationData NotOnOrAfter=\"2013-10-15T04:47:36.913Z\"/></saml2:SubjectConfirmation></saml2:Subject><saml2:AuthnStatement AuthnInstant=\"2013-10-15T02:47:36.917Z\"><saml2:AuthnContext><saml2:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordPotectedTransport</saml2:AuthnContextClassRef></saml2:AuthnContext></saml2:AuthnStatement><saml2:AttributeStatement><saml2:Attribute Name=\"roles\"><saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">observer#5555</saml2:AttributeValue><saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">identity:user-admin#5555</saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name=\"domain\"><saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">1234</saml2:AttributeValue></saml2:Attribute></saml2:AttributeStatement></saml2:Assertion></saml2p:Response>"


    def setupSpec() {
        //initializes open saml. allows us use unmarshaller
        InitializationService.initialize()
        samlSignatureValidator = new SamlSignatureValidator()

        identityConfig.getReloadableConfig() >> reloadableConfig
        identityConfig.getStaticConfig() >> staticConfig

        samlUnmarshaller = new SamlUnmarshaller()
        samlUnmarshaller.identityConfig = identityConfig
    }

    def "validate signature when valid" (){
        given:
        def response = samlUnmarshaller.unmarshallResponse(samlStr)

        when:
        samlSignatureValidator.validateSignature(response.getSignature(), userCertificate)

        then:
        noExceptionThrown()
    }

    def "validate signature when invalid" (){
        given:
        def samlStr = "<saml2p:Response xmlns:saml2p=\"urn:oasis:names:tc:SAML:2.0:protocol\" ID=\"bc1c335f-8078-4769-81a1-bb519194279c\" IssueInstant=\"2013-10-01T15:02:42.110Z\" Version=\"2.0\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
                "   <saml2:Issuer xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\">dedicated</saml2:Issuer>\n" +
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
                "      <saml2:Issuer>dedicated</saml2:Issuer>\n" +
                "      <saml2:Subject>\n" +
                "         <saml2:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified\">john.doe</saml2:NameID>\n" +
                "         <saml2:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\">\n" +
                "            <saml2:SubjectConfirmationData NotOnOrAfter=\"2013-10-01T17:02:42.101Z\"/>\n" +
                "         </saml2:SubjectConfirmation>\n" +
                "      </saml2:Subject>\n" +
                "      <saml2:AuthnStatement AuthnInstant=\"2013-10-01T15:02:42.103Z\">\n" +
                "         <saml2:AuthnContext>\n" +
                "            <saml2:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport</saml2:AuthnContextClassRef>\n" +
                "         </saml2:AuthnContext>\n" +
                "      </saml2:AuthnStatement>\n" +
                "      <saml2:AttributeStatement>\n" +
                "         <saml2:Attribute Name=\"roles\">\n" +
                "            <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">observer#5555</saml2:AttributeValue>\n" +
                "            <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">identity:user-admin#5555</saml2:AttributeValue>\n" +
                "         </saml2:Attribute>\n" +
                "         <saml2:Attribute Name=\"domain\">\n" +
                "            <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">1234</saml2:AttributeValue>\n" +
                "         </saml2:Attribute>\n" +
                "      </saml2:AttributeStatement>\n" +
                "   </saml2:Assertion>\n" +
                "</saml2p:Response>"

        def response = samlUnmarshaller.unmarshallResponse(samlStr)

        when:
        samlSignatureValidator.validateSignature(response.getSignature(), userCertificate)

        then:
        thrown(SignatureValidationException)
    }

    def cleanupSpec() {
        samlUnmarshaller = null;
    }
}