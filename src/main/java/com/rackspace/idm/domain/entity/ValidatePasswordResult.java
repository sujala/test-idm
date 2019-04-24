package com.rackspace.idm.domain.entity;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.PasswordCheckResultTypeEnum;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.PasswordValidityTypeEnum;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class ValidatePasswordResult {
    private String compositionCheckMessage;
    private String blacklistCheckMessage;
    private PasswordCheckResultTypeEnum blacklistCheck;
    private PasswordCheckResultTypeEnum compositionCheck;


    public enum CHECK_TYPES {
        BLACKLIST_CHECK {
            public String toString() {
                return "blacklistCheck";
            }
        }, COMPOSITION_CHECK {
            public String toString() {
                return "compositionCheck";
            }
        };

        public static CHECK_TYPES forValue(String value) {
            try {
                return valueOf(value.toUpperCase());
            } catch (Exception ex) {
                return null;
            }
        }
    }

    public PasswordValidityTypeEnum getValid() {
        // Is TRUE when composition check passes and blacklist check passes or is disabled
        if ((compositionCheck == PasswordCheckResultTypeEnum.PASSED || compositionCheck == PasswordCheckResultTypeEnum.DISABLED) && (blacklistCheck == PasswordCheckResultTypeEnum.PASSED || blacklistCheck == PasswordCheckResultTypeEnum.DISABLED)) {
            return PasswordValidityTypeEnum.TRUE;
        }

        // Is FALSE when composition check fails or blacklist check is failed
        if (compositionCheck == PasswordCheckResultTypeEnum.FAILED || blacklistCheck == PasswordCheckResultTypeEnum.FAILED) {
            return PasswordValidityTypeEnum.FALSE;
        }

        // Is INDETERMINATE as fallback
        return PasswordValidityTypeEnum.INDETERMINATE;
    }

    public List<String>  getNonPassingChecks() {
        List<String> nonPassingCheckNames = new ArrayList<>(2);
        if (compositionCheck != PasswordCheckResultTypeEnum.PASSED  && compositionCheck != PasswordCheckResultTypeEnum.DISABLED) {
            nonPassingCheckNames.add(ValidatePasswordResult.CHECK_TYPES.COMPOSITION_CHECK.toString());
        }

        if (blacklistCheck != PasswordCheckResultTypeEnum.PASSED && blacklistCheck != PasswordCheckResultTypeEnum.DISABLED) {
            nonPassingCheckNames.add(ValidatePasswordResult.CHECK_TYPES.BLACKLIST_CHECK.toString());
        }
        return nonPassingCheckNames;
    }
}



