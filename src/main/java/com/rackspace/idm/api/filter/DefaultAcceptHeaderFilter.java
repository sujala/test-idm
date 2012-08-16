package com.rackspace.idm.api.filter;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * @author john.eo Add a default Accept Header for all rest calls if one is
 *         not present in the original request. Default is XML for all
 *         requests except those in the cloud tree which are JSON
 */
@Component
public class DefaultAcceptHeaderFilter implements ContainerRequestFilter {

    @Override
    public ContainerRequest filter(ContainerRequest request) {
        List<String> acceptStrings = request.getRequestHeaders().get(HttpHeaders.ACCEPT);
        if(acceptStrings == null){
            return request;
        }
        if (acceptStrings.size() > 1 || !acceptStrings.contains("*/*")) {    //If there is an accept header other than */*
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
