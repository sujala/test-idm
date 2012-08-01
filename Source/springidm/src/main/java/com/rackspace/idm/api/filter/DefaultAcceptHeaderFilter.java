package com.rackspace.idm.api.filter;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

/**
 * @author john.eo Add a default Accept Header for all rest calls if one is
 *         not present in the original request. Default is XML for all
 *         requests except those in the cloud tree which are JSON
 */
@Component
public class DefaultAcceptHeaderFilter implements ContainerRequestFilter {

    @Override
    public ContainerRequest filter(ContainerRequest request) {
        if (request.getRequestHeaders().containsKey(HttpHeaders.ACCEPT)) {
            return request;
        }
        
        if (request.getPath().startsWith("cloud")) {
            request.getRequestHeaders().putSingle(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        } else {
            request.getRequestHeaders().putSingle(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML);
        }
        
        return request;
    }

}
