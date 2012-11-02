package com.rackspace.idm.api.resource.pagination;

import com.rackspace.idm.exception.BadRequestException;
import com.unboundid.ldap.sdk.SearchResultEntry;
import lombok.Data;

import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: jacob
 * Date: 29/10/12
 * Time: 09:23
 * To change this template use File | Settings | File Templates.
 */
@Data
public class PaginatorContext<T> {
    private List<SearchResultEntry> searchResultEntryList;
    private List<T> valueList;
    private HashMap<String, String> pageLinks;

    private int offset;
    private int limit;
    private int totalRecords;


    public List<SearchResultEntry> getSearchResultEntryList() {
        if (searchResultEntryList == null) {
            searchResultEntryList = new ArrayList<SearchResultEntry>();
        }
        return searchResultEntryList;
    }

    public void makePageLinks() {
        if (totalRecords > 0) {
            if (offset > totalRecords) {
                throw new BadRequestException(String.format("Offset greater than total number of records (%s)", totalRecords));
            }

            int lastIndex = (totalRecords - limit) < 0 ? 0 : (totalRecords - limit);
            getPageLinks().put("first", String.format("?marker=%s&limit=%s", 0, limit));
            getPageLinks().put("last", String.format("?marker=%s&limit=%s", lastIndex, limit));

            if (withinFirstPage()) {
                getPageLinks().put("prev", getPageLinks().get("first"));
            } else {
                getPageLinks().put("prev", String.format("?marker=%s&limit=%s", offset - limit, limit));
            }

            if (withinLastPage()) {
                getPageLinks().put("next", getPageLinks().get("last"));
            } else {
                getPageLinks().put("next", String.format("?marker=%s&limit=%s", offset + limit, limit));
            }
        }
    }

    protected boolean withinFirstPage() {
        return (0 > this.offset - this.limit);
    }

    protected boolean withinLastPage() {
        return (offset + limit > totalRecords - 1);
    }

    public List<T> getValueList() {
        if (valueList == null) {
            valueList = new ArrayList<T>();
        }
        return valueList;
    }

    public HashMap<String, String> getPageLinks() {
        if (pageLinks == null) {
            pageLinks = new HashMap<String, String>(4);
        }
        return pageLinks;
    }

    public String createLinkHeader(UriInfo uriInfo) {
        if (getPageLinks().size() > 0) {
            StringBuilder linkHeader = new StringBuilder();
            URI path = uriInfo.getAbsolutePath();
            String pathString = path.toString();

            linkHeader.append(makeLink(pathString, pageLinks.get("first"), "first"));
            addComma(linkHeader);
            linkHeader.append(makeLink(pathString, pageLinks.get("prev"), "prev"));
            addComma(linkHeader);
            linkHeader.append(makeLink(pathString, pageLinks.get("next"), "next"));
            addComma(linkHeader);
            linkHeader.append(makeLink(pathString, pageLinks.get("last"), "last"));

            return linkHeader.toString();
        } else {
            return null;
        }
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
