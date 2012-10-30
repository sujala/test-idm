package com.rackspace.idm.api.resource.pagination;

import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.api.resource.pagination.PaginatorContext;
import com.unboundid.ldap.sdk.*;
import com.unboundid.ldap.sdk.controls.ServerSideSortRequestControl;
import com.unboundid.ldap.sdk.controls.SortKey;
import com.unboundid.ldap.sdk.controls.VirtualListViewRequestControl;
import com.unboundid.ldap.sdk.controls.VirtualListViewResponseControl;
import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
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

    @Autowired
    private Configuration config;

    @Override
    public PaginatorContext<T> createSearchRequest(String sortAttribute, SearchRequest searchRequest, int offset, int limit) {
    	int contentCount = 0;
        PaginatorContext<T> context = new PaginatorContext<T>();
        context.setOffset(offset);
        context.setLimit(limit, config.getInt("ldap.paging.limit.default"), config.getInt("ldap.paging.limit.max"));

    	ServerSideSortRequestControl sortRequest = new ServerSideSortRequestControl(new SortKey(sortAttribute));

        VirtualListViewRequestControl vlvRequest = new VirtualListViewRequestControl(context.getOffset(), 0, context.getLimit() - 1, contentCount, null);
        searchRequest.setControls(new Control[]{sortRequest, vlvRequest});

        return context;
    }

    @Override
    public void createPage(SearchResult searchResult, PaginatorContext<T> context) {
        context.getSearchResultEntryList().addAll(searchResult.getSearchEntries());

        try {
            VirtualListViewResponseControl vlvResponseControl = VirtualListViewResponseControl.get(searchResult);
            context.makePageLinks(vlvResponseControl.getContentCount());
        } catch (LDAPException e) {
            context.makePageLinks(0);
        }
    }

}
