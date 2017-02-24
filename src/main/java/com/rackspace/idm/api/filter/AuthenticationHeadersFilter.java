package com.rackspace.idm.api.filter;

import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.api.security.RequestContextHolder;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This filter examines the authentication data for the request and populates
 * the response headers with the corresponding values
 */
@Component
public class AuthenticationHeadersFilter implements ContainerResponseFilter {

    @Autowired
    private RequestContextHolder requestContextHolder;

    @Override
    public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
        if (StringUtils.isNotEmpty(requestContextHolder.getAuthenticationContext().getUsername())) {
            response.getHttpHeaders().putSingle(GlobalConstants.X_USER_NAME, requestContextHolder.getAuthenticationContext().getUsername());
        }
        return response;
    }

}
