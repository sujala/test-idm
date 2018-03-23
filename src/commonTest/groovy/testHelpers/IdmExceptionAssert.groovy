package testHelpers

import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.exception.IdmException
import org.apache.commons.lang.StringUtils

import java.util.regex.Pattern

class IdmExceptionAssert {
    static Pattern PATTERN_ALL = Pattern.compile(".*")

    /**
     * Asserts the provided exception has the specified type, error code, and message that matches the specified regex pattern. Use the
     * {@link #PATTERN_ALL} to match all messages. Use null for the pattern, when the message should be null.
     *
     * @param exception
     * @param expectedIdmExceptionClazz
     * @param expectedErrorCode
     * @param expectedMessagePattern
     */
    static def <T extends IdmException> void assertException(Exception exception, Class<T> expectedIdmExceptionClazz, String expectedErrorCode, Pattern expectedMessagePattern) {

        assert exception.class.isAssignableFrom(expectedIdmExceptionClazz)

        T castException = (T) exception

        assert castException.errorCode == expectedErrorCode

        if (expectedMessagePattern == null) {
            assert castException.message == null
        }
        else {
            assert expectedMessagePattern.matcher(castException.message).matches()
        }
    }

    /**
     * Asserts the provided exception has the specified type, error code, and matches the specified message.
     *
     * @param exception
     * @param expectedIdmExceptionClazz
     * @param expectedErrorCode
     * @param expectedMessage
     */
    static def <T extends IdmException> void assertException(Exception exception, Class<T> expectedIdmExceptionClazz, String expectedErrorCode, String expectedMessage) {
        // If error code is provided must generate the message
        String finalMessage = StringUtils.isNotBlank(expectedErrorCode) ? ErrorCodes.generateErrorCodeFormattedMessage(expectedErrorCode, expectedMessage) : expectedMessage
        assertException(exception, expectedIdmExceptionClazz, expectedErrorCode, Pattern.compile(finalMessage, Pattern.LITERAL))
    }
}
