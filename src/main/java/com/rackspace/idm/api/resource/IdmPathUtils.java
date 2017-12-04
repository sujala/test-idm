package com.rackspace.idm.api.resource;

import com.rackspace.idm.domain.entity.PaginatorContext;
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
    public static Pattern v2TokenValidationPathPattern = Pattern.compile("^cloud/v2.0/tokens/([^/]+)/?$");
    public static Pattern v2TokenEndpointPathPattern = Pattern.compile("^cloud/v2.0/tokens/[^/]+/endpoints/?$");
    public static Pattern v11TokenValidationPathPattern = Pattern.compile("^cloud/v1.1/token/([^/]+)/?$");


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

    /**
     * convention for public documentation is \/*\/*\/public
     * @param requestedPath
     * @return
     */
    public boolean isPublicResource(String requestedPath) {
        return requestedPath.endsWith("public");
    }

    public boolean isCloudResource(String requestedPath) {
        //Cloud v1.0/v1.1/v2.0 checks
        return requestedPath.startsWith("cloud");
    }

    public boolean isDevOpsResource(String requestedPath) {
        return requestedPath.startsWith("devops");
    }

    /**
     * Return whether the request is a v2 Auth request
     *
     * @param request
     * @return
     */
    public boolean isV2AuthenticateResource(ContainerRequest request) {
        // Don't think last endpoint matcher required, but legacy had it so...
        return HttpMethod.POST.equalsIgnoreCase(request.getMethod())
                && request.getPath().startsWith(V2_AUTH_PATH)
                && !v2TokenEndpointPathPattern.matcher(request.getPath()).matches();
    }

    /**
     * Return whether the request is a v2 Validate request.
     *
     * @param request
     * @return
     */
    public boolean isV2ValidateTokenResource(ContainerRequest request) {
        // Don't think last endpoint matcher required, but legacy had it so...
        return HttpMethod.GET.equalsIgnoreCase(request.getMethod())
                && v2TokenValidationPathPattern.matcher(request.getPath()).matches()
                && !v2TokenEndpointPathPattern.matcher(request.getPath()).matches();
    }

    /**
     * Return whether the request is a v1.1 Validate request.
     *
     * @param request
     * @return
     */
    public boolean isV11ValidateTokenResource(ContainerRequest request) {
        return HttpMethod.GET.equalsIgnoreCase(request.getMethod())
                && v11TokenValidationPathPattern.matcher(request.getPath()).matches();
    }

    /**
     * Return whether the request is a v2.0 Revoke token (where token is provided in URL).
     *
     * @param request
     * @return
     */
    public boolean isV2RevokeOtherTokenResource(ContainerRequest request) {
        // Don't think last endpoint matcher required, but legacy had it so...
        return HttpMethod.DELETE.equalsIgnoreCase(request.getMethod())
                && v2TokenValidationPathPattern.matcher(request.getPath()).matches()
                && !v2TokenEndpointPathPattern.matcher(request.getPath()).matches();
    }

    /**
     * Return whether the request is a v1.1 revoke token request.
     *
     * @param request
     * @return
     */
    public boolean isV11RevokeTokenResource(ContainerRequest request) {
        return HttpMethod.DELETE.equalsIgnoreCase(request.getMethod())
                && v11TokenValidationPathPattern.matcher(request.getPath()).matches();
    }

    /**
     * Resource is not protected by authentication. Any unauthenticated caller can retrieve the resource.
     *
     * @param request
     * @return
     */
    public boolean isUnprotectedResource(ContainerRequest request) {
        boolean unprotectedResource = false;

        String path = request.getPath();
        final String method = request.getMethod();

        // skip token authentication for any url that ends with public.
        // convention for public documentation is /*/*/public
        // also if path is /cloud we want to ensure we show the splash page
        // TODO: double check that this is an efficient check and will not cause collisions
        if (isPublicResource(path)
                || path.equals("cloud")
                ) {
            unprotectedResource = true;
        } else if (!isCloudResource(path) && !isDevOpsResource(path)) {
            // Skip authentication for the following matches across all resources
            int index = path.indexOf('/');
            path = index > 0 ? path.substring(index + 1) : ""; //TODO: "/asdf/afafw/fwa" -> "" is correct behavior?

            if (HttpMethod.GET.equals(method) && ("application.wadl".equals(path)
                    || "idm.wadl".equals(path)
                    || path.startsWith("xsd")
                    || path.startsWith("xslt")
                    || "".equals(path)
                    || "tokens".equals(path))) {
                unprotectedResource = true;
            }
        } else if (HttpMethod.GET.equals(method) &&
                (V2_CLOUD_RESOURCE_PATH_PATTERN.matcher(path).matches()
                        || CLOUD_ROOT_RESOURCE_PATH_PATTERN.matcher(path).matches()
                        || V2_CLOUD_EXTENSIONS_RESOURCE_PATH_PATTERN.matcher(path).matches()
                )) {
            // One of the version resources
            unprotectedResource = true;
        }
        return unprotectedResource;
    }

    /**
     * Resource is an auth resource where user provides credentials in exchange for a token.
     *
     * @param request
     * @return
     */
    public boolean isAuthenticationResource(ContainerRequest request) {
        String path = request.getPath();
        final String method = request.getMethod();

        return isV2AuthenticateResource(request)
                || (StringUtils.startsWithIgnoreCase(path, V1_0_AUTH_PATH) && HttpMethod.GET.equals(method))
                || (StringUtils.startsWithIgnoreCase(path, V1_0_AUTH_PATH2) && HttpMethod.GET.equals(method))
                || (StringUtils.startsWithIgnoreCase(path, V1_1_AUTH_PATH) && HttpMethod.POST.equals(method))
                || (StringUtils.startsWithIgnoreCase(path, V1_1_AUTH_ADMIN_PATH) && HttpMethod.POST.equals(method))
                ;
    }

    /**
     * Resource requires caller to be authenticated.
     *
     * @param request
     * @return
     */
    public boolean isProtectedResource(ContainerRequest request) {
        return !isV2AuthenticateResource(request) && !isUnprotectedResource(request);
    }
}
