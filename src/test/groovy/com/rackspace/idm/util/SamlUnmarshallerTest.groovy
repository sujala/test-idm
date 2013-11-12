package com.rackspace.idm.util

import com.rackspace.idm.exception.BadRequestException
import org.joda.time.DateTime
import org.opensaml.saml2.core.*
import spock.lang.Shared
import spock.lang.Specification

class SamlUnmarshallerTest extends Specification {
    @Shared SamlUnmarshaller samlUnmarshaller;

    def setupSpec() {
        org.opensaml.DefaultBootstrap.bootstrap(); //initializes open saml
        samlUnmarshaller = new SamlUnmarshaller();
    }

    def "Unmarshall Saml Response" (){
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
                    "            <saml2:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordPotectedTransport</saml2:AuthnContextClassRef>\n" +
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
        when:
            Response response = samlUnmarshaller.unmarshallResponse(samlStr)

        then:
            response.issuer.value.equals("dedicated")

            response.status.statusCode.value.equals("urn:oasis:names:tc:SAML:2.0:status:Success");

            Assertion assertion = response.assertions.get(0)
            assertion != null;
            assertion.ID.equals("461e5f27-3880-4ef3-a51f-8265ef13b7e6");
            assertion.issueInstant.millis == new DateTime("2013-10-01T15:02:42.107Z").millis
            assertion.version.toString().equals("2.0");
            assertion.issuer.value.equals("dedicated")

            Subject subject = assertion.subject;
            subject != null
            subject.getNameID().value.equals("john.doe")

            SubjectConfirmation subjectConfirmation = subject.subjectConfirmations.get(0);
            subjectConfirmation != null
            subjectConfirmation.method.equals("urn:oasis:names:tc:SAML:2.0:cm:bearer");

            SubjectConfirmationData subjectConfirmationData = subjectConfirmation.subjectConfirmationData;
            subjectConfirmationData != null
            subjectConfirmationData.notOnOrAfter.millis == new DateTime("2013-10-01T17:02:42.101Z").millis

            AuthnStatement authnStatement = assertion.authnStatements.get(0)
            authnStatement != null
            authnStatement.authnInstant.millis == new DateTime("2013-10-01T15:02:42.103Z").millis
            authnStatement.authnContext.authnContextClassRef.authnContextClassRef.equals("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordPotectedTransport")

            AttributeStatement attributeStatement = assertion.attributeStatements.get(0)
            attributeStatement != null

            Attribute roles = attributeStatement.attributes.get(0)
            roles.name == "roles"
            roles.attributeValues.size() == 2

            Attribute domain = attributeStatement.attributes.get(1)
            domain.attributeValues.size() == 1
    }

    def "Unmarshall Saml Response throws exception when syntax wrong" (){
        given:
            def samlStr = "<saml2p:Response xmlns:saml2p=\"urn:oasis:names:tc:SAML:2.0:protocol\" ID=\"bc1c335f-8078-4769-81a1-bb519194279c\" IssueInstant=\"2013-10-01T15:02:42.110Z\" Version=\"2.0\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
                    "   <saml2:Issuer xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\">dedicated</saml2:Issuer>\n" +
                    "   <saml2p:Status>\n" +
                    "      <saml2p:StatusCode Value=\"urn:oasis:names:tc:SAML:2.0:status:Success\"/>\n" +
                    "   </saml2p:Status>\n" +
                    "   saml2:Assertion xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\" ID=\"461e5f27-3880-4ef3-a51f-8265ef13b7e6\" IssueInstant=\"2013-10-01T15:02:42.107Z\" Version=\"2.0\">\n" +
                    "      <saml2:Issuer>dedicated</saml2:Issuer>\n" +
                    "      <saml2:Subject>\n" +
                    "         <saml2:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified\">john.doe</saml2:NameID>\n" +
                    "         <saml2:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\">\n" +
                    "            <saml2:SubjectConfirmationData NotOnOrAfter=\"2013-10-01T17:02:42.101Z\"/>\n" +
                    "         </saml2:SubjectConfirmation>\n" +
                    "      </saml2:Subject>\n" +
                    "   </saml2:Assertion>"
                    "</saml2p:Response>"
        when:
            Response response = samlUnmarshaller.unmarshallResponse(samlStr)

        then:
            thrown(BadRequestException)
    }

    def "Unmarshall Saml Response throws when unknown element present" (){
        given:
        def samlStr = "<saml2p:Response xmlns:saml2p=\"urn:oasis:names:tc:SAML:2.0:protocol\" ID=\"bc1c335f-8078-4769-81a1-bb519194279c\" IssueInstant=\"2013-10-01T15:02:42.110Z\" Version=\"2.0\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
                "   <saml2:Issuer xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\">dedicated</saml2:Issuer>\n" +
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
                "   </saml2:Assertion>\n" +
                "   <Itari>hello</Itari>"
                "</saml2p:Response>"
        when:
            Response response = samlUnmarshaller.unmarshallResponse(samlStr)

        then:
            thrown(BadRequestException)
    }

    def cleanupSpec() {
        samlUnmarshaller = null;
    }
}