package com.rackspace.idm.api.resource;

import com.rackspace.idm.domain.entity.PaginatorContext;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.sun.jersey.spi.container.ContainerRequest;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.regex.Pattern;

/**
 * A utility component to generate/classify resource paths in the IDM environment.
 *
 * This an @Component in order to use IdentityConfig to fix the known issue with creating paths w/ cloud in them.
 */
@Component
public class IdmPathUtils {

    public static String PAGE_FORMAT_STRING = "?marker=%s&limit=%s";
    public static final Pattern V2_CLOUD_RESOURCE_PATH_PATTERN = Pattern.compile("^cloud/v2.0/?$");
    public static final Pattern CLOUD_ROOT_RESOURCE_PATH_PATTERN = Pattern.compile("^cloud/?$");
    public static final Pattern V2_CLOUD_EXTENSIONS_RESOURCE_PATH_PATTERN = Pattern.compile("^cloud/v2.0/extensions/?$");
    public static final String V2_AUTH_PATH = "cloud/v2.0/tokens";
    public static final String V1_0_AUTH_PATH = "cloud/v1.0";
    public static final String V1_0_AUTH_PATH2 = "cloud/auth";
    public static final String V1_1_AUTH_PATH = "cloud/v1.1/auth";
    public static final String V1_1_AUTH_ADMIN_PATH = "cloud/v1.1/auth-admin";

    /**
     * Pattern to recognize validate call against AE or UUID tokens
     */
    public static final String v2TokenValidationPathPatternRegex = "^cloud/v2.0/tokens/([^/]+)/?$";
    public static final String v2TokenEndpointPathPatternRegex = "^cloud/v2.0/tokens/([^/]+)/endpoints/?$";
    public static final String v11TokenValidationPathPatternRegex = "^cloud/v1.1/token/([^/]+)/?$";

    public static final Pattern v2TokenValidationPathPattern = Pattern.compile(v2TokenValidationPathPatternRegex);
    public static final Pattern v2TokenEndpointPathPattern = Pattern.compile(v2TokenEndpointPathPatternRegex);
    public static final Pattern v11TokenValidationPathPattern = Pattern.compile(v11TokenValidationPathPatternRegex);

    /**
     * Creates the generic location header based on the requested location by tacking on id.
     *
     * TODO: Fix this so handles '/cloud' appropriately for staging/production environments where the VIPs are
     * set up to not require 'cloud', but NGINX adds in.
     *
     * @param uriInfo
     * @param id
     * @return
     */
    public URI createLocationHeaderValue(UriInfo uriInfo, String id) {
        UriBuilder requestUriBuilder = uriInfo.getRequestUriBuilder();
        return requestUriBuilder.path(id).build();
    }

    public String createLinkHeader(UriInfo uriInfo, PaginatorContext context) {
        long totalRecords = context.getTotalRecords();
        int offset = context.getOffset();
        int limit = context.getLimit();

        if (totalRecords > 0 && offset <= totalRecords) {
            StringBuilder linkHeader = new StringBuilder();
            URI path = uriInfo.getAbsolutePath();
            String pathString = path.toString();

            if (offset >= limit) {
                linkHeader.append(makeLink(pathString, String.format(PAGE_FORMAT_STRING, 0, limit), "first"));

                addComma(linkHeader);
                linkHeader.append(makeLink(pathString, String.format(PAGE_FORMAT_STRING, offset - limit, limit), "prev"));
            }

            if ((offset + limit) < totalRecords) {
                long lastIndex = getLastIndex(totalRecords, limit, offset);
                addComma(linkHeader);
                linkHeader.append(makeLink(pathString, String.format(PAGE_FORMAT_STRING, lastIndex, limit), "last"));

                addComma(linkHeader);
                linkHeader.append(makeLink(pathString, String.format(PAGE_FORMAT_STRING, offset + limit, limit), "next"));
            }
            return linkHeader.toString();
        } else {
            return null;
        }
    }

    private long getLastIndex(long totalRecords, int limit, int offset) {
        long index = (limit * ((totalRecords - offset) / limit)) + offset;
        if (index == totalRecords) {
            return totalRecords - limit;
        }
        return index;
    }

    private void addComma(final StringBuilder builder) {
        if (builder.length() > 0 ) {
            builder.append(", ");
        }
    }

    private String makeLink(String path, String query, String rel) {
        StringBuilder link = new StringBuilder();
        rel = String.format("\"%s\"", rel);
        link.append("<").append(path).append(query).append(">; rel=").append(rel);
        return link.toString();
    }
}
