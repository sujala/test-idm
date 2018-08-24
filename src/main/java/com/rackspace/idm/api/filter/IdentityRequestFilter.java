package com.rackspace.idm.api.filter;

import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.api.security.RequestContextHolder;
import com.rackspace.idm.api.security.SecurityContext;
import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import java.util.UUID;

@Component
public class IdentityRequestFilter implements ContainerRequestFilter {
    public static final int X_REQUEST_ID_MAX_LENGTH = 64;

    @Context
    private HttpServletRequest req;

    @Autowired
    private RequestContextHolder requestContextHolder;

    @Autowired
    private IdentityConfig identityConfig;

    private final Logger logger = LoggerFactory.getLogger(IdentityRequestFilter.class);

    @Override
    public ContainerRequest filter(ContainerRequest request) {
        initializeRequestContext(request);
        populateMappedDiagnosticContext(request);
        return request;
    }

    /**
     * Initialize the request context to process a new request.
     */
    private void initializeRequestContext(ContainerRequest request) {
        requestContextHolder.getRequestContext().setSecurityContext(new SecurityContext());
        requestContextHolder.getRequestContext().setContainerRequest(request);
    }

    private void populateMappedDiagnosticContext(ContainerRequest request) {
        String path = request.getPath();

        //TODO: Transition this to use the provided ContainerRequest rather than raw HttpServletRequest
        if (req != null) {
            MDC.put(Audit.REMOTE_IP, req.getRemoteAddr());
            MDC.put(Audit.HOST_IP, req.getLocalAddr());
            MDC.put(Audit.PATH, path);
            MDC.put(Audit.GUUID, calculateAuditGuuid(request));
            String xForwardedFor = req.getHeader("X-Forwarded-For");
            if(StringUtils.isNotBlank(xForwardedFor)){
                MDC.put(Audit.X_FORWARDED_FOR, xForwardedFor);
            }else {
                MDC.put(Audit.X_FORWARDED_FOR, req.getRemoteAddr());
            }
        }
    }

    private String calculateAuditGuuid(ContainerRequest request) {
        String requestId = null;
        if (identityConfig.getReloadableConfig().isFeatureUseReposeRequestIdEnabled()) {
            requestId = request.getHeaderValue(GlobalConstants.X_REQUEST_ID);
        }

        if (StringUtils.isBlank(requestId)) {
            requestId = UUID.randomUUID().toString();
        } else if (requestId.length() > X_REQUEST_ID_MAX_LENGTH) {
            requestId = StringUtils.substring(requestId, 0, X_REQUEST_ID_MAX_LENGTH);
        }

        return requestId;
    }
}