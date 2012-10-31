package com.rackspace.idm.api.resource.pagination;

import com.unboundid.ldap.sdk.SearchResultEntry;

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
public class PaginatorContext<T> {
    private List<SearchResultEntry> searchResultEntryList;
    private List<T> valueList;
    private HashMap<String, String> pageLinks;

    private int offset;
    private int limit;


    public List<SearchResultEntry> getSearchResultEntryList() {
        if (searchResultEntryList == null) {
            searchResultEntryList = new ArrayList<SearchResultEntry>();
        }
        return searchResultEntryList;
    }

    public void setSearchResultEntryList(List<SearchResultEntry> searchResultEntries) {
        this.searchResultEntryList = searchResultEntries;
    }


    public int getLimit() {
        return limit;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset < 1 ? 1 : offset;
    }

    public void setLimit(int limit, int limitDefault, int limitMax) {
        if (limit < 1) {
            this.limit = limitDefault;
        } else if (limit >= limitMax) {
            this.limit = limitMax;
        } else {
            this.limit = limit;
        }
    }

    public void makePageLinks(int totalRecords) {
        if (totalRecords > 0) {
            if (offset > totalRecords) {
                offset = 1;
            }

            int lastIndex = (totalRecords - limit) < 0 ? 0 : (totalRecords - limit);
            getPageLinks().put("first", String.format("?marker=%s&limit=%s", 0, limit));
            getPageLinks().put("last", String.format("?marker=%s&limit=%s", lastIndex, limit));

            if (withinFirstPage()) {
                getPageLinks().put("prev", getPageLinks().get("first"));
            } else {
                getPageLinks().put("prev", String.format("?marker=%s&limit=%s", offset - limit, limit));
            }

            if (withinLastPage(totalRecords)) {
                getPageLinks().put("next", getPageLinks().get("last"));
            } else {
                getPageLinks().put("next", String.format("?marker=%s&limit=%s", offset + limit, limit));
            }
        }
    }

    protected boolean withinFirstPage() {
        return (1 > this.offset - this.limit);
    }

    protected boolean withinLastPage(int totalRecords) {
        return (offset + limit > totalRecords - 1);
    }

    public List<T> getValueList() {
        if (valueList == null) {
            valueList = new ArrayList<T>();
        }
        return valueList;
    }

    public void setValueList(List<T> valueList) {
        this.valueList = valueList;
    }

    public HashMap<String, String> getPageLinks() {
        if (pageLinks == null) {
            pageLinks = new HashMap<String, String>(4);
        }
        return pageLinks;
    }

    public void setPageLinks(HashMap<String, String> pageLinks) {
        this.pageLinks = pageLinks;
    }

    public String createLinkHeader(UriInfo uriInfo) {
        if (getValueList().size() > 0) {
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
