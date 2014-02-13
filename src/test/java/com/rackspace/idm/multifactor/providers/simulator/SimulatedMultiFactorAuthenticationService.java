package com.rackspace.idm.multifactor.providers.simulator;

import com.rackspace.identity.multifactor.domain.MfaAuthenticationDecision;
import com.rackspace.identity.multifactor.domain.MfaAuthenticationDecisionReason;
import com.rackspace.identity.multifactor.domain.MfaAuthenticationResponse;
import com.rackspace.identity.multifactor.providers.MultiFactorAuthenticationService;
import com.rackspace.identity.multifactor.providers.duo.domain.DuoAuthResponse;
import com.rackspace.identity.multifactor.providers.duo.domain.DuoResponse;
import com.rackspace.identity.multifactor.providers.duo.domain.DuoStatus;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SimulatedMultiFactorAuthenticationService implements MultiFactorAuthenticationService {

    private HashSet<String> smsRequests = new HashSet<String>();

    @Override
    public void sendSmsPasscodeChallenge(String providerUserId, String providerPhoneId) {
        smsRequests.add(providerUserId + "_" + providerPhoneId);
    }

    public boolean wasSmsPasscodeSentTo(String providerUserId, String providerPhoneId) {
        return smsRequests.contains(providerUserId + "_" + providerPhoneId);
    }

    public Set<String> getSmsPasscodeLog() {
        return smsRequests;
    }

    public void clearSmsPasscodeLog() {
        smsRequests.clear();
    }

    @Override
    public MfaAuthenticationResponse verifyPasscodeChallenge(String providerUserId, String providerPhoneId, final String passcode) {
        final SimulatedPasscode simulatedPasscode = SimulatedPasscode.fromPasscode(passcode);
        if (simulatedPasscode == null) throw new IllegalArgumentException("Must provide a SimulatedPasscode passcode");

        return simulatedPasscode.getResponse();
    }

    public enum SimulatedPasscode {

        ALLOW_ALLOW("allow", MfaAuthenticationDecision.ALLOW, MfaAuthenticationDecisionReason.ALLOW, "allow")
        , ALLOW_BYPASS("bypass", MfaAuthenticationDecision.ALLOW, MfaAuthenticationDecisionReason.BYPASS, "bypass")
        , ALLOW_UNKNOWN("allow_unknown", MfaAuthenticationDecision.ALLOW, MfaAuthenticationDecisionReason.UNKNOWN, "unknown")
        , DENY_TIMEOUT("timeout", MfaAuthenticationDecision.DENY, MfaAuthenticationDecisionReason.TIMEOUT, "timeout")
        , DENY_LOCKEDOUT("lockedout", MfaAuthenticationDecision.DENY, MfaAuthenticationDecisionReason.LOCKEDOUT, "locked out")
        , DENY_PROVIDER_FAILURE("providerfailure", MfaAuthenticationDecision.DENY, MfaAuthenticationDecisionReason.PROVIDER_FAILURE, "providerfailure")
        , DENY_PROVIDER_UNAVAILABLE("providerunavailable", MfaAuthenticationDecision.DENY, MfaAuthenticationDecisionReason.PROVIDER_UNAVAILABLE, "providerunavailable")
        , DENY_UNKNOWN("deny_unknown", MfaAuthenticationDecision.DENY, MfaAuthenticationDecisionReason.UNKNOWN, "unknown")
        , DENY_DENY("deny", MfaAuthenticationDecision.DENY, MfaAuthenticationDecisionReason.DENY, "deny");

        private String passcode;
        private MfaAuthenticationResponse response;

        private SimulatedPasscode(final String passcode, final MfaAuthenticationDecision decision, final MfaAuthenticationDecisionReason reason, final String message) {
            this.passcode = passcode;
            response = new MfaAuthenticationResponse<DuoResponse<DuoAuthResponse>>() {
                @Override
                public MfaAuthenticationDecision getDecision() {
                    return decision;
                }

                @Override
                public MfaAuthenticationDecisionReason getDecisionReason() {
                    return reason;
                }

                @Override
                public String getMessage() {
                    return message;
                }

                /**
                 * hardcode to just return ok response for now...
                 *
                 * @return
                 */
                @Override
                public DuoResponse<DuoAuthResponse> getProviderResponse() {
                    return new DuoResponse<DuoAuthResponse>(DuoStatus.OK, new DuoAuthResponse("result", "status", "status message"));
                }
            };
        }

        public String getPasscode() {
            return passcode;
        }

        public MfaAuthenticationResponse getResponse() {
            return response;
        }

        public static SimulatedPasscode fromPasscode(String passcode) {
            for (SimulatedPasscode simulatedPasscode : values()) {
                if (simulatedPasscode.passcode.equals(passcode)) {
                    return simulatedPasscode;
                }
            }
            return null;
        }

    }
}
