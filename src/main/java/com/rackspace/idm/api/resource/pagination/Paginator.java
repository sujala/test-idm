package com.rackspace.idm.api.resource.pagination;

import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.rackspace.idm.api.resource.pagination.PaginatorContext;

import javax.ws.rs.core.UriInfo;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: jacob
 * Date: 10/17/12
 * Time: 9:40 AM
 * To change this template use File | Settings | File Templates.
 */

public interface Paginator<T> {

    PaginatorContext<T> createSearchRequest(String sortAttribute, SearchRequest searchRequest, int offset, int limit);

    void createPage(SearchResult searchResult, PaginatorContext<T> context);

    String createLinkHeader(UriInfo uriInfo, PaginatorContext<T> context);
}
