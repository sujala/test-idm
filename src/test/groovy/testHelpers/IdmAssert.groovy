package testHelpers

import com.sun.jersey.api.client.ClientResponse
import org.openstack.docs.identity.api.v2.IdentityFault

import javax.ws.rs.core.MediaType
import javax.xml.bind.JAXBElement

/**
 * Holds various higher level assertions that can be used in spock tests. Patterned after JUnit's Assert class.
 */
class IdmAssert {

    static def <T extends com.rackspace.api.common.fault.v1.Fault> void assertRackspaceCommonFaultResponse(ClientResponse clientResponse, Class<T> expectedTypeClazz, int expectedStatus, String expectedMessage) {
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

}
