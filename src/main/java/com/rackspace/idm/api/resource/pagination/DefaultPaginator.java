package com.rackspace.idm.api.resource.pagination;

import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.api.resource.pagination.PaginatorContext;
import com.unboundid.ldap.sdk.*;
import com.unboundid.ldap.sdk.controls.ServerSideSortRequestControl;
import com.unboundid.ldap.sdk.controls.SortKey;
import com.unboundid.ldap.sdk.controls.VirtualListViewRequestControl;
import com.unboundid.ldap.sdk.controls.VirtualListViewResponseControl;
import org.springframework.stereotype.Component;

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
    public PaginatorContext<T> createSearchRequest(String sortAttribute, SearchRequest searchRequest, int offset, int limit) {
    	int contentCount = 0;
        PaginatorContext<T> context = new PaginatorContext<T>();
        context.setOffset(offset);
        context.setLimit(limit);

    	ServerSideSortRequestControl sortRequest = new ServerSideSortRequestControl(new SortKey(sortAttribute));

        if (context.getOffset() == 0) {
            offset = 1;
        }

        VirtualListViewRequestControl vlvRequest = new VirtualListViewRequestControl(offset, 0, context.getLimit() - 1, contentCount, null);
        searchRequest.setControls(sortRequest, vlvRequest);

        return context;
    }

    @Override
    public void createPage(SearchResult searchResult, PaginatorContext<T> context) {
        context.getSearchResultEntryList().addAll(searchResult.getSearchEntries());

        try {
            VirtualListViewResponseControl vlvResponseControl = VirtualListViewResponseControl.get(searchResult);
            context.setTotalRecords(vlvResponseControl.getContentCount());
            context.makePageLinks();
        } catch (LDAPException e) {
            context.setTotalRecords(0);
            context.makePageLinks();
        }
    }

}
