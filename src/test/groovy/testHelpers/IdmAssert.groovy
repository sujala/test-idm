package testHelpers

import com.rackspace.idm.exception.IdmException
import com.sun.jersey.api.client.ClientResponse
import org.apache.commons.lang.StringUtils
import org.openstack.docs.identity.api.v2.IdentityFault

import javax.ws.rs.core.MediaType
import javax.xml.bind.JAXBElement
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Holds various higher level assertions that can be used in spock tests. Patterned after JUnit's Assert class.
 */
class IdmAssert {

    def static PATTERN_ALL = Pattern.compile(".*")

    static def <T extends com.rackspace.api.common.fault.v1.Fault> void assertRackspaceCommonFaultResponse(ClientResponse clientResponse, Class<T> expectedTypeClazz, int expectedStatus, String expectedMessage) {
        T fault = clientResponse.getEntity(expectedTypeClazz)
        assert clientResponse.status == expectedStatus
        assert fault.message == expectedMessage
        assert fault.code == expectedStatus
    }

    /**
     * Use to verify Faults that should be returned in response to v1.1 API calls have the appropriate type, status code, and message.
     *
     * Note - when processing JSON responses be sure the appropriate JSON readers exist for the expected fault type. Otherwise
     * the Fault object returned will just contain nulls. See com.rackspace.idm.api.resource.cloud.v11.json.readers.JSONReaderForForbiddenFault.
     *
     * @param clientResponse
     * @param expectedTypeClazz
     * @param expectedStatus
     * @param expectedMessage
     */
    static def <T extends com.rackspacecloud.docs.auth.api.v1.AuthFault> void assertV1AuthFaultResponse(ClientResponse clientResponse, Class<T> expectedTypeClazz, int expectedStatus, String expectedMessage) {
        T fault = clientResponse.getEntity(expectedTypeClazz)
        assert clientResponse.status == expectedStatus
        assert fault.message == expectedMessage
        assert fault.code == expectedStatus
    }

    static def <T extends IdentityFault> void assertOpenStackV2FaultResponse(ClientResponse clientResponse, Class<T> expectedTypeClazz, int expectedStatus, String expectedMessage) {
        T fault = null

        if (clientResponse.getType() == MediaType.APPLICATION_XML_TYPE) {
            JAXBElement<T> responseBody = clientResponse.getEntity(expectedTypeClazz)
            assert responseBody.value.class.isAssignableFrom(expectedTypeClazz)
            fault = responseBody.value
        }
        else if (clientResponse.getType() == MediaType.APPLICATION_JSON_TYPE) {
            fault = clientResponse.getEntity(expectedTypeClazz)
        }

        assert clientResponse.status == expectedStatus
        assert fault.message == expectedMessage
        assert fault.code == expectedStatus
    }

    static def <T extends IdentityFault> void assertOpenStackV2FaultResponseWithMessagePattern(ClientResponse clientResponse, Class<T> expectedTypeClazz, int expectedStatus, Pattern expectedMessagePattern) {
        T fault = null

        if (clientResponse.getType() == MediaType.APPLICATION_XML_TYPE) {
            JAXBElement<T> responseBody = clientResponse.getEntity(expectedTypeClazz)
            assert responseBody.value.class.isAssignableFrom(expectedTypeClazz)
            fault = responseBody.value
        }
        else if (clientResponse.getType() == MediaType.APPLICATION_JSON_TYPE) {
            fault = clientResponse.getEntity(expectedTypeClazz)
        }

        assert clientResponse.status == expectedStatus
        assert fault.code == expectedStatus
        if (expectedMessagePattern == null) {
            assert fault.message == null
        }
        else {
            assert expectedMessagePattern.matcher(fault.message).matches()
        }
    }

    static def <T extends IdentityFault> void assertOpenStackV2FaultResponseWithErrorCode(ClientResponse clientResponse, Class<T> expectedTypeClazz, int expectedStatus, String errorCode) {
        assertOpenStackV2FaultResponseWithMessagePattern(clientResponse, expectedTypeClazz, expectedStatus, generateErrorCodePattern(errorCode))
    }

    /**
     * The provided pattern will be used to generate a regex using {@link #generateErrorCodePattern(java.lang.String, java.lang.String)} if
     * a non-null error code is specified
     *
     * @param actualException
     * @param expectedTypeClazz
     * @param expectedErrorCode
     * @param expectedErrorMessagePattern A regex formatted string
     */
    static <T extends IdmException> void assertIdmExceptionWithMessagePattern(Throwable actualException, Class<T> expectedTypeClazz, String expectedErrorCode, String expectedErrorMessagePattern) {
        Pattern errorMessagePattern;
        if (StringUtils.isNotBlank()) {
            errorMessagePattern = generateErrorCodePattern(expectedErrorCode, expectedErrorMessagePattern)
        } else {
            errorMessagePattern = Pattern.compile(expectedErrorMessagePattern)
        }

        assert actualException.class.isAssignableFrom(expectedTypeClazz)

        IdmException exception = (IdmException) actualException
        assert exception.errorCode == expectedErrorCode
        assert errorMessagePattern.matcher(exception.getMessage())
    }

    /**
     * This is the required format for messages that return error codes. This allows consumers to parse error messages
     * for specific issues without having the message changed underneath them. Only the first part of the message is
     * guaranteed per contract. After the ';', change is fair game.
     *
     * @param errorCode
     * @return
     */
    static Pattern generateErrorCodePattern(String errorCode) {
        Pattern.compile(String.format("^Error code: '%s';.*", errorCode))
    }

    static Pattern generateErrorCodePattern(String errorCode, String errorMessagePattern) {
        Pattern.compile(String.format("^Error code: '%s'; %s", errorCode, errorMessagePattern))
    }
}
