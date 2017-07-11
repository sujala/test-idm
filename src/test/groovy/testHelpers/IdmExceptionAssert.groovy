package testHelpers

import com.rackspace.idm.exception.IdmException

import java.util.regex.Pattern

class IdmExceptionAssert {
    static Pattern PATTERN_ALL = Pattern.compile(".*")

    /**
     * Asserts the provided exception has the specified type, error code, and matches the specified pattern. Use the
     * {@link #PATTERN_ALL} to match all messages. Use null for the pattern, when the message should be null.
     *
     * @param exception
     * @param expectedIdmExceptionClazz
     * @param expectedErrorCode
     * @param expectedMessagePattern
     */
    static def <T extends IdmException> void assertException(Exception exception, Class<T> expectedIdmExceptionClazz, String expectedErrorCode, Pattern expectedMessagePattern) {

        assert exception.class.isAssignableFrom(expectedIdmExceptionClazz)

        T castException = (T) exception;

        assert castException.errorCode == expectedErrorCode

        if (expectedMessagePattern == null) {
            assert castException.message == null
        }
        else {
            assert expectedMessagePattern.matcher(castException.message).matches()
        }
    }
}
