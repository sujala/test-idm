package com.rackspace.idm.util;

import com.rackspace.idm.domain.entity.SamlLogoutResponse;
import org.apache.commons.lang.StringUtils;
import org.opensaml.saml.saml2.core.LogoutResponse;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.core.StatusMessage;
import org.opensaml.saml.saml2.core.impl.LogoutResponseBuilder;
import org.opensaml.saml.saml2.core.impl.StatusBuilder;
import org.opensaml.saml.saml2.core.impl.StatusCodeBuilder;
import org.opensaml.saml.saml2.core.impl.StatusMessageBuilder;
import org.springframework.util.Assert;

public final class SamlLogoutResponseUtil {

    /**
     * private constructor cause this is a util class that should not be instantiated.
     */
    private SamlLogoutResponseUtil() {
    }

    public static SamlLogoutResponse createSuccessfulLogoutResponse(String inResponseTo) {
        LogoutResponse logoutResponse =  new LogoutResponseBuilder().buildObject();

        if (StringUtils.isNotBlank(inResponseTo)) {
            logoutResponse.setInResponseTo(inResponseTo);
        }

        Status status = new StatusBuilder().buildObject();
        logoutResponse.setStatus(status);

        //set the status code
        StatusCode code = new StatusCodeBuilder().buildObject();
        code.setValue(StatusCode.SUCCESS);
        status.setStatusCode(code);

        return new SamlLogoutResponse(logoutResponse);
    }

    public static SamlLogoutResponse createErrorLogoutResponse(String inResponseTo, String samlStatusCode, String statusMessage, Exception exceptionThrown) {
        Assert.notNull(samlStatusCode);

        LogoutResponse logoutResponse =  new LogoutResponseBuilder().buildObject();

        if (StringUtils.isNotBlank(inResponseTo)) {
            logoutResponse.setInResponseTo(inResponseTo);
        }

        Status status = new StatusBuilder().buildObject();
        logoutResponse.setStatus(status);

        //set the status code
        StatusCode code = new StatusCodeBuilder().buildObject();
        code.setValue(samlStatusCode);
        status.setStatusCode(code);


        if (StringUtils.isNotBlank(statusMessage)) {
            StatusMessage message = new StatusMessageBuilder().buildObject();
            message.setMessage(statusMessage);
            status.setStatusMessage(message);
        }

        return new SamlLogoutResponse(logoutResponse, exceptionThrown);
    }

}
