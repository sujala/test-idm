package com.rackspace.idm.api.resource.pagination;

import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.api.resource.pagination.PaginatorContext;
import com.rackspace.idm.exception.BadRequestException;
import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.ldap.sdk.*;
import com.unboundid.ldap.sdk.controls.ServerSideSortRequestControl;
import com.unboundid.ldap.sdk.controls.SortKey;
import com.unboundid.ldap.sdk.controls.VirtualListViewRequestControl;
import com.unboundid.ldap.sdk.controls.VirtualListViewResponseControl;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.UriInfo;
import java.net.URI;

/**
 * Created with IntelliJ IDEA.
 * User: jacob
 * Date: 10/10/12
 * Time: 5:22 PM
 * To change this template use File | Settings | File Templates.
 */

@Component
public class DefaultPaginator<T> implements Paginator<T> {

    static String PAGE_FORMAT_STRING = "?marker=%s&limit=%s";

    @Override
    public PaginatorContext<T> createSearchRequest(String sortAttribute, SearchRequest searchRequest, int offset, int limit) {
        return createSearchRequest(sortAttribute, searchRequest, null, offset, limit);
    }

    @Override
    public PaginatorContext<T> createSearchRequest(String sortAttribute, SearchRequest searchRequest, ASN1OctetString contextId, int offset, int limit) {
        int contentCount = 0;
        PaginatorContext<T> context = new PaginatorContext<T>();
        context.setOffset(offset);
        context.setLimit(limit);

    	ServerSideSortRequestControl sortRequest = new ServerSideSortRequestControl(true, new SortKey(sortAttribute));

        VirtualListViewRequestControl vlvRequest = new VirtualListViewRequestControl(offset + 1, 0, context.getLimit() - 1, contentCount, contextId, true);
        searchRequest.setControls(sortRequest, vlvRequest);

        return context;
    }

    @Override
    public void createPage(SearchResult searchResult, PaginatorContext<T> context) {
        context.getSearchResultEntryList().addAll(searchResult.getSearchEntries());

        try {
            VirtualListViewResponseControl vlvResponseControl = VirtualListViewResponseControl.get(searchResult);
            context.setTotalRecords(vlvResponseControl.getContentCount());
        } catch (LDAPException e) {
            context.setTotalRecords(context.getSearchResultEntryList().size());
        } catch (NullPointerException npe) {
            context.setTotalRecords(context.getSearchResultEntryList().size());
        }
    }

    @Override
    public String createLinkHeader(UriInfo uriInfo, PaginatorContext<T> context) {
        int totalRecords = context.getTotalRecords();
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
                int lastIndex = getLastIndex(totalRecords, limit, offset);
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

    protected int getLastIndex(int totalRecords, int limit, int offset) {
        int index = (limit * ((totalRecords - offset) / limit)) + offset;
        if (index == totalRecords) {
            return totalRecords - limit;
        }
        return index;
    }

    protected void addComma(final StringBuilder builder) {
        if (builder.length() > 0 ) {
            builder.append(", ");
        }
    }

    protected String makeLink(String path, String query, String rel) {
        StringBuilder link = new StringBuilder();
        rel = String.format("\"%s\"", rel);
        link.append("<").append(path).append(query).append(">; rel=").append(rel);
        return link.toString();
    }
}
