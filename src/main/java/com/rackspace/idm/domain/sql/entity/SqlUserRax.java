package com.rackspace.idm.domain.sql.entity;

import lombok.Data;
import org.dozer.Mapping;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

@Data
@Entity
@Table(name = "user_rax")
public class SqlUserRax {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "password_last_updated")
    private Date passwordLastUpdated = new Date();

    @Column(name = "password_self_updated")
    private boolean passwordWasSelfUpdated;

    @Column(name = "secret_question")
    private String encryptedSecretQuestion;

    @Column(name = "secret_answer")
    private String encryptedSecretAnswer;

    @Column(name = "secret_question_id")
    private String encryptedSecretQuestionId;

    @Column(name = "api_key")
    private String encryptedApiKey;

    @Column(name = "display_name")
    private String encryptedDisplayName;

    @Column(name = "nast_id")
    private String nastId;

    @Mapping("defaultRegion")
    @Column(name = "region")
    private String region;

    @Column(name = "created", updatable = false, insertable = false)
    private Date created;

    @Column(name = "updated", updatable = false, insertable = false)
    private Date updated;

    @Column(name = "salt")
    private String salt;

    @Column(name = "mfa_mobile_phone_id")
    private String multiFactorMobilePhoneRsId;

    @Column(name = "mfa_device_pin")
    private String multiFactorDevicePin;

    @Column(name = "mfa_device_pin_expiration")
    private Date multiFactorDevicePinExpiration;

    @Column(name = "mfa_device_verified")
    private Boolean multiFactorDeviceVerified;

    @Mapping("multiFactorEnabled")
    @Column(name = "mfa_enabled")
    private Boolean multifactorEnabled;

    @Column(name = "mfa_external_user_id")
    private String externalMultiFactorUserId;

    @Column(name = "mfa_state")
    private String multiFactorState;

    @Column(name = "mfa_enforcement_level")
    private String userMultiFactorEnforcementLevel;

    @Column(name = "token_format")
    private String tokenFormat;

    @Column(name = "mfa_type")
    private String multiFactorType;

    @Column(name = "mfa_last_failed_attempt")
    private Date multiFactorLastFailedTimestamp;

    @Column(name = "mfa_failed_attempt_count")
    private Integer multiFactorFailedAttemptCount;

    @Column(name = "password_failure_date")
    private Date passwordFailureDate;

    @Column(name = "client_id")
    private String clientId;

    @Column(name = "encryption_version")
    private String encryptionVersion;

    @Column(name = "contact_id")
    private String contactId;

}