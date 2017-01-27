package testHelpers.saml

import com.rackspace.idm.SAMLConstants
import net.shibboleth.utilities.java.support.xml.SerializeSupport
import org.joda.time.DateTime
import org.opensaml.core.config.InitializationService;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml2.core.*;
import org.opensaml.saml.saml2.core.impl.*
import org.opensaml.core.xml.XMLObject
import org.opensaml.core.xml.schema.XSAny;
import org.opensaml.core.xml.schema.XSString
import org.opensaml.core.xml.schema.impl.XSAnyBuilder;
import org.opensaml.core.xml.schema.impl.XSStringBuilder
import org.opensaml.security.credential.Credential;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.opensaml.xmlsec.signature.support.Signer;
import org.opensaml.xmlsec.signature.impl.SignatureBuilder;
import org.w3c.dom.Element

import java.util.*;


public class SamlProducer {

    private Credential credential;

	private SamlCredentialUtils samlCredentialUtils = new SamlCredentialUtils();

	SamlProducer(String privateKeyLocation, String publicKeyLocation) {
        credential = samlCredentialUtils.getSigningCredential(publicKeyLocation, privateKeyLocation)
    }

	SamlProducer(Credential credential) {
        this.credential = credential
    }

    public Response createSAMLResponse(final String subjectId, final DateTime authenticationTime,
			                           final Map<String, List<Object>> attributes, String issuer, Integer samlAssertionSeconds,
									   String authnContextClassRef = SAMLConstants.PASSWORD_PROTECTED_AUTHCONTEXT_REF_CLASS, DateTime issueInstant = new DateTime()) {
		
		try {
			InitializationService.initialize();
			
			Signature signature = createSignature();
			Status status = createStatus();
			Issuer responseIssuer = null;
			Issuer assertionIssuer = null;
			Subject subject = null;
			AttributeStatement attributeStatement = null;
			
			if (issuer != null) {
				responseIssuer = createIssuer(issuer);
				assertionIssuer = createIssuer(issuer);
			}
			
			if (subjectId != null) {
				subject = createSubject(subjectId, samlAssertionSeconds);
			}
			
			if (attributes != null && attributes.size() != 0) {
				attributeStatement = createAttributeStatement(attributes);
			}
			
			AuthnStatement authnStatement = createAuthnStatement(authenticationTime, authnContextClassRef);
			
			Assertion assertion = createAssertion(new DateTime(), subject, assertionIssuer, authnStatement, attributeStatement);
			
			Response response = createResponse(issueInstant, responseIssuer, status, assertion);
			response.setSignature(signature);
			
			ResponseMarshaller marshaller = new ResponseMarshaller();
			Element element = marshaller.marshall(response);
			
			if (signature != null) {
				Signer.signObject(signature);
			}
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			SerializeSupport.writeNode(element, baos);
		
			return response;
			
		} catch (Throwable t) {
			t.printStackTrace();
			return null;
		}
	}

	public LogoutRequest createSAMLLogoutRequest(final String subjectId, String issuer, DateTime issueInstant = new DateTime()) {
		try {
			InitializationService.initialize();

			Signature signature = createSignature();
			Issuer logoutRequestIssuer = null;

			if (issuer != null) {
				logoutRequestIssuer = createIssuer(issuer);
			}

			// create name element
			NameIDBuilder nameIdBuilder = new NameIDBuilder();
			NameID nameId = nameIdBuilder.buildObject();
			nameId.setValue(subjectId );
			nameId.setFormat("urn:oasis:names:tc:SAML:2.0:nameid-format:persistent");

			LogoutRequestBuilder builder = new LogoutRequestBuilder();
			LogoutRequest logoutRequest = builder.buildObject();

			logoutRequest.setNameID(nameId)
			logoutRequest.setIssueInstant(issueInstant)
			logoutRequest.setIssuer(logoutRequestIssuer)

			logoutRequest.setSignature(signature);

			LogoutRequestMarshaller marshaller = new LogoutRequestMarshaller();
			Element element = marshaller.marshall(logoutRequest);

			if (signature != null) {
				Signer.signObject(signature);
			}

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			SerializeSupport.writeNode(element, baos);

			return logoutRequest;

		} catch (Throwable t) {
			t.printStackTrace();
			return null;
		}
	}
	
	public void setPrivateKeyLocation(String privateKeyLocation) {
		this.privateKeyLocation = privateKeyLocation;
	}

	public void setPublicKeyLocation(String publicKeyLocation) {
		this.publicKeyLocation = publicKeyLocation;
	}
	
	private Response createResponse(final DateTime issueDate, Issuer issuer, Status status, Assertion assertion) {
		ResponseBuilder responseBuilder = new ResponseBuilder();
		Response response = responseBuilder.buildObject();
		response.setID(UUID.randomUUID().toString());
		response.setIssueInstant(issueDate);
		response.setVersion(SAMLVersion.VERSION_20);
		response.setIssuer(issuer);
		response.setStatus(status);
		response.getAssertions().add(assertion);
		return response;
	}
	
	private Assertion createAssertion(final DateTime issueDate, Subject subject, Issuer issuer, AuthnStatement authnStatement,
			                          AttributeStatement attributeStatement) {
		AssertionBuilder assertionBuilder = new AssertionBuilder();
		Assertion assertion = assertionBuilder.buildObject();
		assertion.setID(UUID.randomUUID().toString());
		assertion.setIssueInstant(issueDate);
		assertion.setSubject(subject);
		assertion.setIssuer(issuer);
		
		if (authnStatement != null)
			assertion.getAuthnStatements().add(authnStatement);
		
		if (attributeStatement != null)
			assertion.getAttributeStatements().add(attributeStatement);
		
		return assertion;
	}
	
	private Issuer createIssuer(final String issuerName) {
		// create Issuer object
		IssuerBuilder issuerBuilder = new IssuerBuilder();
		Issuer issuer = issuerBuilder.buildObject();
		issuer.setValue(issuerName);	
		return issuer;
	}
	
	private Subject createSubject(final String subjectId, final Integer samlAssertionSeconds) {
		DateTime currentDate = new DateTime();
		if (samlAssertionSeconds != null)
			currentDate = currentDate.plusSeconds(samlAssertionSeconds);
		
		// create name element
		NameIDBuilder nameIdBuilder = new NameIDBuilder(); 
		NameID nameId = nameIdBuilder.buildObject();
		nameId.setValue(subjectId );
		nameId.setFormat("urn:oasis:names:tc:SAML:2.0:nameid-format:persistent");
	
		SubjectConfirmationDataBuilder dataBuilder = new SubjectConfirmationDataBuilder();
		SubjectConfirmationData subjectConfirmationData = dataBuilder.buildObject();
		subjectConfirmationData.setNotOnOrAfter(currentDate);
		
		SubjectConfirmationBuilder subjectConfirmationBuilder = new SubjectConfirmationBuilder();
		SubjectConfirmation subjectConfirmation = subjectConfirmationBuilder.buildObject();
		subjectConfirmation.setMethod("urn:oasis:names:tc:SAML:2.0:cm:bearer");
		subjectConfirmation.setSubjectConfirmationData(subjectConfirmationData);
		
		// create subject element
		SubjectBuilder subjectBuilder = new SubjectBuilder();
		Subject subject = subjectBuilder.buildObject();
		subject.setNameID(nameId);
		subject.getSubjectConfirmations().add(subjectConfirmation);
		
		return subject;
	}
	
	private AuthnStatement createAuthnStatement(final DateTime issueDate, String authnContextClassRef) {
		// create authcontextclassref object
		AuthnContextClassRefBuilder classRefBuilder = new AuthnContextClassRefBuilder();
		AuthnContextClassRef classRef = classRefBuilder.buildObject();
		classRef.setAuthnContextClassRef(authnContextClassRef);
		
		// create authcontext object
		AuthnContextBuilder authContextBuilder = new AuthnContextBuilder();
		AuthnContext authnContext = authContextBuilder.buildObject();
		authnContext.setAuthnContextClassRef(classRef);
		
		// create authenticationstatement object
		AuthnStatementBuilder authStatementBuilder = new AuthnStatementBuilder();
		AuthnStatement authnStatement = authStatementBuilder.buildObject();
		authnStatement.setAuthnInstant(issueDate);
		authnStatement.setAuthnContext(authnContext);
		
		return authnStatement;
	}
	
	private AttributeStatement createAttributeStatement(Map<String, List<Object>> attributes) {
		// create authenticationstatement object
		AttributeStatementBuilder attributeStatementBuilder = new AttributeStatementBuilder();
		AttributeStatement attributeStatement = attributeStatementBuilder.buildObject();
		
		AttributeBuilder attributeBuilder = new AttributeBuilder();
		if (attributes != null) {
			for (Map.Entry<String, List<Object>> entry : attributes.entrySet()) {
				Attribute attribute = attributeBuilder.buildObject();
				attribute.setName(entry.getKey());
				
				for (Object value : entry.getValue()) {
					XMLObject xmlObject = null;
					if (value instanceof String) {
						XSStringBuilder stringBuilder = new XSStringBuilder();
						XSString attributeValue = stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);
						attributeValue.setValue(value);
						xmlObject = attributeValue
					} else {
						XSAnyBuilder builder = new XSAnyBuilder();
						XSAny attributeValue = builder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME);
						attributeValue.setTextContent(value.toString());
						xmlObject = attributeValue
					}
					attribute.getAttributeValues().add(xmlObject);
				}
				
				attributeStatement.getAttributes().add(attribute);
			}
		}
		
		return attributeStatement;
	}
	
	private Status createStatus() {
		StatusCodeBuilder statusCodeBuilder = new StatusCodeBuilder();
		StatusCode statusCode = statusCodeBuilder.buildObject();
		statusCode.setValue(StatusCode.SUCCESS);
		
		StatusBuilder statusBuilder = new StatusBuilder();
		Status status = statusBuilder.buildObject();
		status.setStatusCode(statusCode);
		
		return status;
	}
	
	private Signature createSignature() throws Throwable {
		if (credential != null) {
			SignatureBuilder builder = new SignatureBuilder();
			Signature signature = builder.buildObject();
			signature.setSigningCredential(credential);
			signature.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1);
			signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
			
			return signature;
		}
		
		return null;
	}
}
