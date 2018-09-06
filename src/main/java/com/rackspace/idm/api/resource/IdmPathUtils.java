package com.rackspace.idm.api.resource;

import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.PaginatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

/**
 * A utility component to generate/classify resource paths in the IDM environment.
 *
 * This an @Component in order to use IdentityConfig to fix the known issue with creating paths w/ cloud in them.
 */
@Component
public class IdmPathUtils {

    public static String PAGE_FORMAT_STRING = "?marker=%s&limit=%s";
    public static String CLOUD_PATH = "cloud";

    public enum Environment { DEV, STAGING, PROD };

    @Autowired
    private IdentityConfig identityConfig;

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

        /* Uncomment to fix CID-170.

        // If environment is set to staging or production, then remove "cloud" segment from path
        String environment = identityConfig.getReloadableConfig().getIdentityDeploymentEnvironment();
        if (environment.equalsIgnoreCase(Environment.STAGING.name()) || environment.equalsIgnoreCase(Environment.PROD.name())) {
            requestUriBuilder.replacePath(uriInfo.getPath().replace(CLOUD_PATH + "/",""));
        }
        */

        return requestUriBuilder.path(id).build();
    }

    /**
     * Creates the generic location header based on the requested location by the specified paths.
     *
     * NOTE: This method will only work correctly for cloud resources.
     *
     * @param uriInfo
     * @param paths
     * @return
     */
    public URI createLocationHeaderValue(UriInfo uriInfo, String... paths) {
        UriBuilder requestUriBuilder = uriInfo.getRequestUriBuilder();

        // Replace the existing path with just the base uri of the request.
        requestUriBuilder.replacePath(uriInfo.getBaseUri().getPath());

        // Add cloud path since it is part of the base cloud resource uri.
        requestUriBuilder.path(CLOUD_PATH);

        // Build new path
        for (String path : paths) {
            requestUriBuilder.path(path);
        }

        // If environment is set to staging or production, then remove "cloud" segment from path
        String environment = identityConfig.getReloadableConfig().getIdentityDeploymentEnvironment();
        if (environment.equalsIgnoreCase(Environment.STAGING.name()) || environment.equalsIgnoreCase(Environment.PROD.name())) {
            requestUriBuilder.replacePath(requestUriBuilder.build().getPath().replace(CLOUD_PATH + "/",""));
        }

        return requestUriBuilder.build();
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
