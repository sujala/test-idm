package com.rackspace.idm.api.resource;

import com.rackspace.idm.api.resource.pagination.DefaultPaginator;
import com.rackspace.idm.domain.entity.PaginatorContext;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

/**
 * A utility component to generate paths in the IDM environment. Make this a component from the getgo as anticipate
 * use of IdentityConfig to fix the known issue with creating paths w/ cloud in them.
 */
@Component
public class IdmPathUtils {

    public static String PAGE_FORMAT_STRING = "?marker=%s&limit=%s";


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
