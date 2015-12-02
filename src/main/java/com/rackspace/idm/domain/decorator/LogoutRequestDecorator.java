package com.rackspace.idm.domain.decorator;

import com.rackspace.idm.ErrorCodes;
import com.rackspace.idm.exception.BadRequestException;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.opensaml.saml2.core.LogoutRequest;
import org.opensaml.xml.signature.Signature;

public class LogoutRequestDecorator {
    private LogoutRequest logoutRequest;

    public LogoutRequestDecorator(LogoutRequest logoutRequest) {
        this.logoutRequest = logoutRequest;
    }

    public LogoutRequest getLogoutRequest() {
        return logoutRequest;
    }

    public String checkAndGetIssuer() {
        String issuer = getIssuer();
        if (StringUtils.isBlank(issuer)) {
            throw new BadRequestException(ErrorCodes.generateErrorCodeFormattedMessage(ErrorCodes.ERROR_CODE_FEDERATION_INVALID_ISSUER, "Issuer is not specified"));
        }
        return issuer;
    }

    public String getIssuer() {
        if (logoutRequest.getIssuer() == null || StringUtils.isBlank(logoutRequest.getIssuer().getValue())) {
            return null;
        }
        return logoutRequest.getIssuer().getValue();
    }

    public Signature checkAndGetSignature() {
        Signature sig = getSignature();
        if (sig == null) {
            throw new BadRequestException(ErrorCodes.generateErrorCodeFormattedMessage(ErrorCodes.ERROR_CODE_FEDERATION_MISSING_SIGNATURE, "No Signature specified"));
        }
        return sig;
    }

    public Signature getSignature() {
        return logoutRequest.getSignature();
    }

    public String checkAndGetUsername() {
        String username = getUsername();
        if (StringUtils.isBlank(username)) {
            throw new BadRequestException(ErrorCodes.generateErrorCodeFormattedMessage(ErrorCodes.ERROR_CODE_FEDERATION_MISSING_USERNAME, "Username is not specified"));
        }
        return username;
    }

    public String getUsername() {
        if (logoutRequest.getNameID() == null || StringUtils.isBlank(logoutRequest.getNameID().getValue())) {
            return null;
        }

        return logoutRequest.getNameID().getValue();
    }

    public DateTime checkAndGetIssueInstant() {
        DateTime issueInstant = logoutRequest.getIssueInstant();

        if (issueInstant == null) {
            throw new BadRequestException(ErrorCodes.generateErrorCodeFormattedMessage(ErrorCodes.ERROR_CODE_FEDERATION_MISSING_AUTH_INSTANT, "IssueInstant is not specified"));
        }

        return issueInstant;
    }

}
