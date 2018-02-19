package com.rackspace.idm.api.resource.cloud;

import lombok.Getter;

public enum NewRelicTransactionNames {
    V2Auth("/v2.0/tokens (POST Authenticate)")
    , V2AuthWithPwd("/v2.0/tokens (POST Authenticate Password)")
    , V2AuthWithPwdRcn("/v2.0/tokens (POST Authenticate Password RCN)")
    , V2AuthWithToken("/v2.0/tokens (POST Authenticate Token)")
    , V2AuthWithTokenDelegation("/v2.0/tokens (POST Authenticate Token + Delegation)")
    , V2AuthWithApi("/v2.0/tokens (POST Authenticate API)")
    , V2AuthMfaFirst("/v2.0/tokens (POST Authenticate Mfa First Handshake)")
    , V2AuthMfaFirstSms("/v2.0/tokens (POST Authenticate Mfa First Handshake SMS)")
    , V2AuthMfaFirstOtp("/v2.0/tokens (POST Authenticate Mfa First Handshake OTP)")
    , V2AuthMfaSecond("/v2.0/tokens (POST Authenticate Mfa Second Handshake)")
    , V2AuthMfaSecondSms("/v2.0/tokens (POST Authenticate Mfa Second Handshake SMS)")
    , V2AuthMfaSecondOtp("/v2.0/tokens (POST Authenticate Mfa Second Handshake OTP)")
    , V2AuthRackerPwd("/v2.0/tokens (POST Authenticate Racker Password)")
    , V2AuthRackerRsa("/v2.0/tokens (POST Authenticate Racker RSA)")
    , V2Validate("/v2.0/tokens (GET Validate)")
    , V2ValidateRacker("/v2.0/tokens (GET Validate Racker)")
    , V2ValidateFederatedRacker("/v2.0/tokens (GET Validate Federated Racker)")
    , V2ValidateDomain("/v2.0/tokens (GET Validate Domain)")
    , V2ValidateDomainRcn("/v2.0/tokens (GET Validate Domain Rcn)")
    , V2ValidateFederatedDomain("/v2.0/tokens (GET Validate Federated Domain)")
    , V2ValidateImpersonation("/v2.0/tokens (GET Validate Impersonation)")
    ;

    @Getter
    private String transactionName;

    NewRelicTransactionNames(String transactionName) {
        this.transactionName = transactionName;
    }
}
