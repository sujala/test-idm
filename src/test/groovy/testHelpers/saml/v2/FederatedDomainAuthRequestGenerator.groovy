package testHelpers.saml.v2

import net.shibboleth.utilities.java.support.xml.SerializeSupport
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.collections4.MapUtils
import org.apache.commons.lang.StringUtils
import org.apache.log4j.Logger
import org.joda.time.DateTime
import org.opensaml.core.config.InitializationService
import org.opensaml.core.xml.XMLObject
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport
import org.opensaml.core.xml.schema.XSAny
import org.opensaml.core.xml.schema.XSString
import org.opensaml.core.xml.schema.impl.XSAnyBuilder
import org.opensaml.core.xml.schema.impl.XSStringBuilder
import org.opensaml.saml.common.SAMLVersion
import org.opensaml.saml.saml2.core.*
import org.opensaml.saml.saml2.core.impl.*
import org.opensaml.security.credential.Credential
import org.opensaml.xmlsec.signature.Signature
import org.opensaml.xmlsec.signature.impl.SignatureBuilder
import org.opensaml.xmlsec.signature.support.SignatureConstants
import org.opensaml.xmlsec.signature.support.SignatureValidator
import org.opensaml.xmlsec.signature.support.Signer
import org.w3c.dom.Element
import testHelpers.saml.SamlCredentialUtils

class FederatedDomainAuthRequestGenerator {
    private static final Logger logger = Logger.getLogger(FederatedDomainAuthRequestGenerator.class)

    private Credential brokerCredential
    private Credential originCredential

    private SamlCredentialUtils samlCredentialUtils = new SamlCredentialUtils()

    AssertionMarshaller assertionMarshaller = new AssertionMarshaller()
    ResponseMarshaller responseMarshaller = new ResponseMarshaller()
    ResponseUnmarshaller responseUnmarshaller = new ResponseUnmarshaller()

    FederatedDomainAuthRequestGenerator(String brokerPublicKeyLocation, String brokerPrivateKeyLocation, String originPublicKeyLocation, String originPrivateKeyLocation) {
        this.brokerCredential = samlCredentialUtils.getSigningCredential(brokerPublicKeyLocation, brokerPrivateKeyLocation)
        this.originCredential = samlCredentialUtils.getSigningCredential(originPublicKeyLocation, originPrivateKeyLocation)
        InitializationService.initialize()
    }

    FederatedDomainAuthRequestGenerator(Credential brokerCredential, Credential originCredential) {
        this.brokerCredential = brokerCredential
        this.originCredential = originCredential
        InitializationService.initialize()
    }

    def convertResponseToString(Response samlResponse) {
        ResponseMarshaller marshaller = new ResponseMarshaller()
        Element element = marshaller.marshall(samlResponse)

        ByteArrayOutputStream baos = new ByteArrayOutputStream()
        SerializeSupport.writeNode(element, baos)
        return new String(baos.toByteArray())
    }

    public Response createSignedSAMLResponse(FederatedDomainAuthGenerationRequest genRequest) {
        Response resp = createUnsignedSAMLResponse(genRequest)
        signSAMLResponse(resp, brokerCredential)
        return resp
    }

    public Response createUnsignedSAMLResponse(FederatedDomainAuthGenerationRequest genRequest) {
        try {
            HashMap<String, List<String>> attributes = createAttributes(genRequest)
            Assertion brokerAssertion = createUnsignedAssertion(genRequest.brokerIssuer, genRequest.requestIssueInstant, genRequest.username, genRequest.samlAuthContext.samlAuthnContextClassRef, genRequest.validitySeconds, attributes)
            Assertion originAssertion = createUnsignedAssertion(genRequest.originIssuer, genRequest.requestIssueInstant, genRequest.username, genRequest.samlAuthContext.samlAuthnContextClassRef, genRequest.validitySeconds, attributes)

            signAssertion(originAssertion, originCredential)

            Issuer responseIssuer = null
            if (genRequest.brokerIssuer != null) {
                responseIssuer = createIssuer(genRequest.brokerIssuer)
            }
            Status status = createStatus()
            Response response = createResponse(genRequest.requestIssueInstant, responseIssuer, status, [brokerAssertion, originAssertion])

            return response
        } catch (Throwable t) {
            logger.error(t)
            return null
        }
    }

    public Response signSAMLResponse(Response response, Credential signingCredential) {
        Signature signature = createSignature(signingCredential)
        response.setSignature(signature)

        /*
        The signing operation operates on the underlying cached DOM representation of the object. Therefore, the SAML
        object to be signed must be marshalled before the actual signature computation is performed.
        */
        responseMarshaller.marshall(response)
        if (signature != null) {
            Signer.signObject(signature)
        }

        SignatureValidator.validate(signature, signingCredential)

        /*
        After marshalling the response signatures on the assertion will no longer validate due to reference issues. You'll get
        something like:

         Caused by: org.opensaml.xmlsec.signature.support.SignatureException: Apache xmlsec IdResolver could not
         resolve the Element for id reference: d65cf6c1-eb0e-4654-81dc-469509d38145

         The id referenced will be the the SAML Assertion. I've read there is some bug in the cached DOM, but couldn't
          find a specific reference. However, simply unmarshalling the DOM of the response after signing the response
          will allow both signatures to be validated.
         */
        responseUnmarshaller.unmarshall(response.getDOM())

        return response
    }

    private HashMap<String, List<String>> createAttributes(FederatedDomainAuthGenerationRequest genRequest) {
        HashMap<String, List<String>> attributes = new HashMap<String, List<String>>()
        if (StringUtils.isNotBlank(genRequest.email)) {
            attributes.put("email", [genRequest.email])
        }
        if (StringUtils.isNotBlank(genRequest.domainId)) {
            attributes.put("domain", [genRequest.domainId])
        }
        if (org.apache.commons.collections.CollectionUtils.isNotEmpty(genRequest.roleNames)) {
            attributes.put("roles", genRequest.roleNames)
        }
        attributes.putAll(genRequest.otherAttributes)
        return attributes
    }

    public Assertion createUnsignedAssertion(String issuer, DateTime authTime, String username, String authnContextClassRef, int ttlSeconds, Map<String, List<String>> attributes) {
        Issuer sIssuer = null
        Subject subject = null
        AttributeStatement attributeStatement = null

        if (issuer != null) {
            sIssuer = createIssuer(issuer)
        }
        if (username != null) {
            subject = createSubject(username, ttlSeconds)
        }
        if (MapUtils.isNotEmpty(attributes)) {
            attributeStatement = createAttributeStatement(attributes)
        }
        AuthnStatement authnStatement = createAuthnStatement(authTime, authnContextClassRef)

        Assertion assertion = createAssertion(new DateTime(), subject, sIssuer, authnStatement, attributeStatement)
        return assertion
    }

    public Assertion signAssertion(Assertion assertion, Credential signingCredential) {
        Signature signature = createSignature(signingCredential)
        assertion.setSignature(signature)

        /*
        The signing operation operates on the underlying cached DOM representation of the object. Therefore, the SAML
        object to be signed must be marshalled before the actual signature computation is performed.
         */
        assertionMarshaller.marshall(assertion)

        if (signature != null) {
            Signer.signObject(signature)
        }

        SignatureValidator.validate(signature, signingCredential)

        return assertion
    }

    private Response createResponse(
            final DateTime issueDate, Issuer issuer, Status status, List<Assertion> assertions) {
        ResponseBuilder responseBuilder = new ResponseBuilder()
        Response response = responseBuilder.buildObject()
        response.setID(UUID.randomUUID().toString())
        response.setIssueInstant(issueDate)
        response.setVersion(SAMLVersion.VERSION_20)
        response.setIssuer(issuer)
        response.setStatus(status)
        response.getAssertions().addAll(assertions)
        return response
    }

    private Assertion createAssertion(
            final DateTime issueDate, Subject subject, Issuer issuer, AuthnStatement authnStatement,
            AttributeStatement attributeStatement) {
        AssertionBuilder assertionBuilder = new AssertionBuilder()
        Assertion assertion = assertionBuilder.buildObject()
        assertion.setID(UUID.randomUUID().toString())
        assertion.setIssueInstant(issueDate)
        assertion.setSubject(subject)
        assertion.setIssuer(issuer)

        if (authnStatement != null)
            assertion.getAuthnStatements().add(authnStatement)

        if (attributeStatement != null)
            assertion.getAttributeStatements().add(attributeStatement)

        return assertion
    }

    private Issuer createIssuer(final String issuerName) {
        // create Issuer object
        IssuerBuilder issuerBuilder = new IssuerBuilder()
        Issuer issuer = issuerBuilder.buildObject()
        issuer.setValue(issuerName)
        return issuer
    }

    private Subject createSubject(final String subjectId, final Integer samlAssertionSeconds) {
        DateTime currentDate = new DateTime()
        if (samlAssertionSeconds != null)
            currentDate = currentDate.plusSeconds(samlAssertionSeconds)

        // create name element
        NameIDBuilder nameIdBuilder = new NameIDBuilder()
        NameID nameId = nameIdBuilder.buildObject()
        nameId.setValue(subjectId)
        nameId.setFormat(org.opensaml.saml.saml2.core.NameIDType.PERSISTENT)

        SubjectConfirmationDataBuilder dataBuilder = new SubjectConfirmationDataBuilder()
        SubjectConfirmationData subjectConfirmationData = dataBuilder.buildObject()
        subjectConfirmationData.setNotOnOrAfter(currentDate)

        SubjectConfirmationBuilder subjectConfirmationBuilder = new SubjectConfirmationBuilder()
        SubjectConfirmation subjectConfirmation = subjectConfirmationBuilder.buildObject()
        subjectConfirmation.setMethod(org.opensaml.saml.saml2.core.SubjectConfirmation.METHOD_BEARER)
        subjectConfirmation.setSubjectConfirmationData(subjectConfirmationData)

        // create subject element
        SubjectBuilder subjectBuilder = new SubjectBuilder()
        Subject subject = subjectBuilder.buildObject()
        subject.setNameID(nameId)
        subject.getSubjectConfirmations().add(subjectConfirmation)

        return subject
    }

    private AuthnStatement createAuthnStatement(final DateTime issueDate, String authnContextClassRef) {
        // create authcontextclassref object
        AuthnContextClassRefBuilder classRefBuilder = new AuthnContextClassRefBuilder()
        AuthnContextClassRef classRef = classRefBuilder.buildObject()
        classRef.setAuthnContextClassRef(authnContextClassRef)

        // create authcontext object
        AuthnContextBuilder authContextBuilder = new AuthnContextBuilder()
        AuthnContext authnContext = authContextBuilder.buildObject()
        authnContext.setAuthnContextClassRef(classRef)

        // create authenticationstatement object
        AuthnStatementBuilder authStatementBuilder = new AuthnStatementBuilder()
        AuthnStatement authnStatement = authStatementBuilder.buildObject()
        authnStatement.setAuthnInstant(issueDate)
        authnStatement.setAuthnContext(authnContext)

        return authnStatement
    }

    private AttributeStatement createAttributeStatement(Map<String, List<Object>> attributes) {
        // create authenticationstatement object
        AttributeStatementBuilder attributeStatementBuilder = new AttributeStatementBuilder()
        AttributeStatement attributeStatement = attributeStatementBuilder.buildObject()

        AttributeBuilder attributeBuilder = new AttributeBuilder()
        if (attributes != null) {
            for (Map.Entry<String, List<Object>> entry : attributes.entrySet()) {
                Attribute attribute = attributeBuilder.buildObject()
                attribute.setName(entry.getKey())

                for (Object value : entry.getValue()) {
                    XMLObject xmlObject = null
                    if (value instanceof String) {
                        XSStringBuilder stringBuilder = new XSStringBuilder()
                        XSString attributeValue = stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME)
                        attributeValue.setValue(value)
                        xmlObject = attributeValue
                    } else {
                        XSAnyBuilder builder = new XSAnyBuilder()
                        XSAny attributeValue = builder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME)
                        attributeValue.setTextContent(value.toString())
                        xmlObject = attributeValue
                    }
                    attribute.getAttributeValues().add(xmlObject)
                }

                attributeStatement.getAttributes().add(attribute)
            }
        }

        return attributeStatement
    }

    private Status createStatus() {
        StatusCodeBuilder statusCodeBuilder = new StatusCodeBuilder()
        StatusCode statusCode = statusCodeBuilder.buildObject()
        statusCode.setValue(StatusCode.SUCCESS)

        StatusBuilder statusBuilder = new StatusBuilder()
        Status status = statusBuilder.buildObject()
        status.setStatusCode(statusCode)

        return status
    }

    private Signature createSignature(Credential signingCredential) throws Throwable {
        if (signingCredential != null) {
            SignatureBuilder builder = new SignatureBuilder()
            Signature signature = builder.buildObject()
            signature.setSigningCredential(signingCredential)
            signature.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1)
            signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS)

            return signature
        }

        return null
    }
}
