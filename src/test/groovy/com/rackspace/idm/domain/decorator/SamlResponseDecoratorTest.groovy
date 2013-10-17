package com.rackspace.idm.domain.decorator

import com.rackspace.idm.util.SamlUnmarshaller
import org.opensaml.DefaultBootstrap
import spock.lang.Shared
import spock.lang.Specification

class SamlResponseDecoratorTest extends Specification {
    @Shared def IDP_URI = "http://my.test.idp"
    @Shared def USERNAME = "john.doe"

    @Shared def samlStr
    @Shared def samlResponse
    @Shared def samlResponseDecorator


    def setupSpec(){
        DefaultBootstrap.bootstrap()

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
                "         <saml2:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified\">" + USERNAME + "</saml2:NameID>\n" +
                "         <saml2:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\">\n" +
                "            <saml2:SubjectConfirmationData NotOnOrAfter=\"2013-10-01T17:02:42.101Z\"/>\n" +
                "         </saml2:SubjectConfirmation>\n" +
                "      </saml2:Subject>\n" +
                "      <saml2:AuthnStatement AuthnInstant=\"2013-10-01T15:02:42.103Z\">\n" +
                "         <saml2:AuthnContext>\n" +
                "            <saml2:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordPotectedTransport</saml2:AuthnContextClassRef>\n" +
                "         </saml2:AuthnContext>\n" +
                "      </saml2:AuthnStatement>\n" +
                "      <saml2:AttributeStatement>\n" +
                "         <saml2:Attribute Name=\"roles\">\n" +
                "            <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">observer</saml2:AttributeValue>\n" +
                "            <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">identity:user-admin#5555</saml2:AttributeValue>\n" +
                "         </saml2:Attribute>\n" +
                "         <saml2:Attribute Name=\"domain\">\n" +
                "            <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">1234</saml2:AttributeValue>\n" +
                "         </saml2:Attribute>\n" +
                "      </saml2:AttributeStatement>\n" +
                "   </saml2:Assertion>\n" +
                "</saml2p:Response>"

        samlResponse = new SamlUnmarshaller().unmarshallResponse(samlStr)

        samlResponseDecorator = new SamlResponseDecorator(samlResponse)
    }

    def "Get saml response" () {
        when:
        def result = samlResponseDecorator.getSamlResponse()

        then:
        result == samlResponse
    }

    def "Get idp uri" () {
        when:
        def result = samlResponseDecorator.getIdpUri()

        then:
        result == IDP_URI
    }


    def "Get attribute that exists" () {
        when:
        def result = samlResponseDecorator.getAttribute("roles")

        then:
        result.size() == 2
    }

    def "Get attribute that does not exist" () {
        when:
        def result = samlResponseDecorator.getAttribute("fakeAttribute")

        then:
        result == null
    }
}

