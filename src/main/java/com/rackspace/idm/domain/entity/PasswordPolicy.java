package com.rackspace.idm.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.base.Charsets;
import com.rackspace.idm.exception.InvalidPasswordPolicyException;
import lombok.Getter;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.time.Duration;
import java.time.format.DateTimeParseException;

/**
 * Represents a generic password policy.
 */
@JsonRootName(value = "passwordPolicy")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class PasswordPolicy {
    private static final Logger logger = LoggerFactory.getLogger(PasswordPolicy.class);

    public static final String INVALID_DURATION_MSG = "Password durations must be positive";
    public static final String INVALID_HISTORY_MSG = "Password history restriction must be positive";
    public static final String INVALID_POLICY_GENERIC_MSG = "Error parsing policy";

    /**
     * How long a given password can be used before it must be changed. Uses ISO-8601 durations.
     */
    @Nullable
    private String passwordDuration;

    /**
     * How many previous passwords to check new passwords against. The new password must not match one of these passwords
     * or the current password. A value of 0 (or null) means the new password is only checked against the current password
     */
    @Nullable
    private Integer passwordHistoryRestriction;

    @JsonIgnore
    private Duration passwordDurationAsDuration;

    /**
     * Creates a policy with the provided values. Either (or both) arguments may be null.
     *
     * @param passwordDuration if not null, must be a positive duration
     * @param passwordHistoryRestriction if not null, must be >= 0
     * @throws InvalidPasswordPolicyException if restrictions are not met
     *
     */
    public PasswordPolicy(@JsonProperty("passwordDuration") String passwordDuration, @JsonProperty("passwordHistoryRestriction") Integer passwordHistoryRestriction) {
        this.passwordDuration = passwordDuration;
        this.passwordHistoryRestriction = passwordHistoryRestriction;

        if (StringUtils.isNotBlank(passwordDuration)) {
            try {
                passwordDurationAsDuration = Duration.parse(passwordDuration);
                if (passwordDurationAsDuration.toMillis() < 0) {
                    throw new InvalidPasswordPolicyException(INVALID_DURATION_MSG);
                }
            } catch (DateTimeParseException e) {
                throw new InvalidPasswordPolicyException(INVALID_DURATION_MSG);
            }
        }

        if (passwordHistoryRestriction != null && passwordHistoryRestriction < 0) {
            throw new InvalidPasswordPolicyException(INVALID_HISTORY_MSG);
        }
    }

    public static PasswordPolicy fromJson(String json) throws IOException {
        if (StringUtils.isEmpty(json)) {
            return null;
        }
        try {
            PasswordPolicy policy = getMapper().readValue(json, PasswordPolicy.class);
            return policy;
        } catch (Exception e) {
            if (e.getCause() instanceof InvalidPasswordPolicyException) {
                throw (InvalidPasswordPolicyException) e.getCause();
            }
            throw new InvalidPasswordPolicyException(INVALID_POLICY_GENERIC_MSG, e);
        }
    }

    public static PasswordPolicy fromBytes(byte[] bytes) {
        if (ArrayUtils.isEmpty(bytes)) {
            return null;
        }

        PasswordPolicy policy = null;
        if (ArrayUtils.isNotEmpty(bytes)) {
            String policyStr = new String(bytes, Charsets.UTF_8);
            try {
                policy = PasswordPolicy.fromJson(policyStr);
            } catch (IOException e) {
                logger.error(String.format("Error converting password policy '%s' to policy object. Returning null", policyStr), e);
            }
        }
        return policy;
    }

    public String toJson() throws JsonProcessingException {
        String json = getMapper().writeValueAsString(this);

//        if (passwordDuration != null) {
//            String rawDuration = passwordDuration.toString();
//            if (json.contains(rawDuration)) {
//                String formattedDuration = getDurationAsString();
//                json = json.replace(rawDuration, formattedDuration);
//            }
//        }

        return json;
    }

    public int calculateEffectivePasswordHistoryRestriction() {
        return passwordHistoryRestriction != null ? passwordHistoryRestriction : 0;
    }

    public byte[] toJsonBytes() throws JsonProcessingException {
        return toJson().getBytes(Charsets.UTF_8);
    }

    private String getReducedDurationAsString() {
        String finalRep = null;
        if (passwordDurationAsDuration != null) {
            long days = passwordDurationAsDuration.toDays();
            if (days != 0) {
                Duration withoutDays = passwordDurationAsDuration.minusDays(days);
                String daysDurationRep = "P" + days + "D";
                if (withoutDays.isZero()) {
                    // Duration is reducable to exact number of days
                    finalRep = daysDurationRep;
                } else {
                    // When duration is reduced, have some days and some extra. Subtract the days to format remaining
                    String dS = passwordDurationAsDuration.minusDays(days).toString(); // PT12H3M2S
                    finalRep = dS.replaceFirst("P", daysDurationRep);
                }
            } else {
                finalRep = passwordDurationAsDuration.toString();
            }
        }
        return finalRep;
    }

    //TODO Can use this as a static variable
    private static ObjectMapper getMapper() {
        ObjectMapper mapper = new ObjectMapper().registerModule(new Jdk8Module()).registerModule(new JavaTimeModule());
        mapper.enable(DeserializationFeature.UNWRAP_ROOT_VALUE);
        mapper.enable(SerializationFeature.WRAP_ROOT_VALUE);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
