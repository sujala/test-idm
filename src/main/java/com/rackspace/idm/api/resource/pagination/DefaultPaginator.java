package com.rackspace.idm.api.resource.pagination;

import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.UriInfo;
import java.util.List;


/**
 * Created with IntelliJ IDEA.
 * User: jacob
 * Date: 10/10/12
 * Time: 5:22 PM
 * To change this template use File | Settings | File Templates.
 */

@Component
public class DefaultPaginator<T> implements Paginator<T> {

    @Override
    public String createLinkHeader(UriInfo uriInfo) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<T> valueList() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void valueList(List<T> list) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public SearchRequest createSearchRequestWithPaging(String sortAttribute, SearchRequest searchRequest, int offset, int limit) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void createPageFromResult(SearchResult searchResult, int offset, int limit) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<SearchResultEntry> searchResultEntries() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
