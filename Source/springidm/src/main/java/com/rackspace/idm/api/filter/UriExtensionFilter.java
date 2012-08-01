package com.rackspace.idm.api.filter;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class UriExtensionFilter implements ContainerRequestFilter {
    private static final String DOT = ".";

    @SuppressWarnings("serial")
    final static Map<String, String> EXTENSION_TO_ACCEPT_HEADER = new HashMap<String, String>() {
        {
            put("json", MediaType.APPLICATION_JSON);
            put("xml", MediaType.APPLICATION_XML);
        }
    };

    @Override
    public ContainerRequest filter(ContainerRequest request) {
        // the path to the request without query params
        final String absolutePath = request.getAbsolutePath().toString();

        final String extension = StringUtils.substringAfterLast(absolutePath,
            DOT);
        if (shouldFilter("/" + StringUtils.difference(request.getBaseUri().toString(),absolutePath), extension)) {
            request.getRequestHeaders().putSingle(HttpHeaders.ACCEPT, EXTENSION_TO_ACCEPT_HEADER.get(extension));
            final String absolutePathWithoutExtension = StringUtils.substringBeforeLast(absolutePath, DOT);
            request.setUris(request.getBaseUri(),getRequestUri(absolutePathWithoutExtension,request.getQueryParameters()));
        }
        return request;
    }

    boolean shouldFilter(String restPath, String extension) {
        return EXTENSION_TO_ACCEPT_HEADER.containsKey(extension);
    }

    URI getRequestUri(String absolutePathWithoutExtension,
        Map<String, List<String>> queryParams) {
        final UriBuilder requestUriBuilder = UriBuilder.fromPath(absolutePathWithoutExtension);
        for (Map.Entry<String, List<String>> queryParamEntry : queryParams.entrySet()) {
            for (String value : queryParamEntry.getValue()) {
                requestUriBuilder.queryParam(queryParamEntry.getKey(), value);
            }
        }
        return requestUriBuilder.build();
    }
}
