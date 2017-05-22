package com.rackspace.idm.domain.entity

import com.rackspace.idm.exception.InvalidPasswordPolicyException
import spock.lang.Specification
import spock.lang.Unroll

class PasswordPolicyTest extends Specification {

    /**
     * Tests serializing and unserializing various valid password policies. expectedIfDifferent is only provided
     * if different from the 'from' value. See
     * <a href="https://docs.oracle.com/javase/8/docs/api/java/time/Duration.html#parse-java.lang.CharSequence-">Duration Javadoc</a>
     * for valid inputs.
     *
     * @return
     */
    @Unroll
    def "Can serialize/unserialize provided policies as expected: from: #from; expectedIfDifferent: #expectedIfDifferent"() {
        given:
        // a null expected means the expected is exactly the same as from
        def expectedOut = expectedIfDifferent != null ? expectedIfDifferent : from

        expect:
        expectedOut.equals(PasswordPolicy.fromJson(from).toJson())

        where:
        from | expectedIfDifferent
        // nothing
        "{\"passwordPolicy\":{}}" | null
        "{\"passwordPolicy\":{\"passwordDuration\":\" \"}}" | null
        "{\"passwordPolicy\":{\"passwordDuration\": null}}" | "{\"passwordPolicy\":{}}"
        "{\"passwordPolicy\":{\"passwordHistoryRestriction\":\" \"}}" | "{\"passwordPolicy\":{}}"
        "{\"passwordPolicy\":{\"passwordHistoryRestriction\": null}}" | "{\"passwordPolicy\":{}}"

        // Durations by self
        "{\"passwordPolicy\":{\"passwordDuration\":\"P90D\"}}" | null
        "{\"passwordPolicy\":{\"passwordDuration\":\"P90DT1H\"}}" | null
        "{\"passwordPolicy\":{\"passwordDuration\":\"P5DT5H1M3S\"}}" | null
        "{\"passwordPolicy\":{\"passwordDuration\":\"PT0.000000001S\"}}" | null

        // If user supplied value can be reduced (e.g. hours > 24), we reduce it
        "{\"passwordPolicy\":{\"passwordDuration\":\"PT90H\"}}" | null
        "{\"passwordPolicy\":{\"passwordDuration\":\"PT61M\"}}" | null

        // Inner spaces are removed
        "{\"passwordPolicy\": {\"passwordDuration\": \"P90D\"}}" | "{\"passwordPolicy\":{\"passwordDuration\":\"P90D\"}}"

        // History by self
        "{\"passwordPolicy\":{\"passwordHistoryRestriction\":2}}" | null
        "{\"passwordPolicy\":{\"passwordHistoryRestriction\":0}}" | null
        "{\"passwordPolicy\":{\"passwordHistoryRestriction\":10}}" | null

        // While API limits to configured value, the policy itself makes no restrictions beyond being positive
        "{\"passwordPolicy\":{\"passwordHistoryRestriction\":15}}" | null

        // Accepts full
        "{\"passwordPolicy\":{\"passwordDuration\":\"P5DT5H1M3S\",\"passwordHistoryRestriction\":2}}" | null
        "{\"passwordPolicy\":{\"passwordDuration\":\"P5DT5H1M3.5S\",\"passwordHistoryRestriction\":1}}" | null
        "{\"passwordPolicy\":{\"passwordDuration\":\"P5DT5H1M3S\",\"passwordHistoryRestriction\":2}}" | null

        // Show order is always output as duration -> history
        "{\"passwordPolicy\":{\"passwordHistoryRestriction\":2,\"passwordDuration\":\"P5DT5H1M3S\"}}" | "{\"passwordPolicy\":{\"passwordDuration\":\"P5DT5H1M3S\",\"passwordHistoryRestriction\":2}}"
    }

    /**
     * See
     * <a href="https://docs.oracle.com/javase/8/docs/api/java/time/Duration.html#parse-java.lang.CharSequence-">Duration Javadoc</a>
     * for valid inputs. We also don't support negative durations for this particular use case.
     *
     * @return
     */
    @Unroll
    def "Invalid password policy durations are rejected: policy: #policy"() {
        when:
        PasswordPolicy.fromJson(policy).toJson()

        then:
        thrown(InvalidPasswordPolicyException)

        where:
        policy | errorMsg
        // Week, Month, Year durations are not allowed
        "{\"passwordPolicy\":{\"passwordDuration\":\"P1W\"}}" | PasswordPolicy.INVALID_POLICY_GENERIC_MSG
        "{\"passwordPolicy\":{\"passwordDuration\":\"P1M\"}}" | PasswordPolicy.INVALID_POLICY_GENERIC_MSG
        "{\"passwordPolicy\":{\"passwordDuration\":\"P1Y\"}}" | PasswordPolicy.INVALID_POLICY_GENERIC_MSG


        // Negative durations not allowed
        "{\"passwordPolicy\":{\"passwordDuration\":\"P-1D\"}}" | PasswordPolicy.INVALID_DURATION_MSG

        // Negative history not allowed
        "{\"passwordPolicy\":{\"passwordHistoryRestriction\":-1}}" | PasswordPolicy.INVALID_HISTORY_MSG
    }

}
