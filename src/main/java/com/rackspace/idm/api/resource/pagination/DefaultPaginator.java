package com.rackspace.idm.api.resource.pagination;

import com.rackspace.idm.domain.entity.User;
import com.unboundid.ldap.sdk.*;
import com.unboundid.ldap.sdk.controls.ServerSideSortRequestControl;
import com.unboundid.ldap.sdk.controls.SortKey;
import com.unboundid.ldap.sdk.controls.VirtualListViewRequestControl;
import com.unboundid.ldap.sdk.controls.VirtualListViewResponseControl;
import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

    private List<SearchResultEntry> searchResultEntryList;
    private List<T> valueList;
    private HashMap<String, String> pages;

    private int offset;
    private int limit;

    @Override
    public void createSearchRequest(String sortAttribute, SearchRequest searchRequest, int offset, int limit) {
    	int contentCount = 0;

    	offset(offset);
    	limit(limit);

    	ServerSideSortRequestControl sortRequest = new ServerSideSortRequestControl(new SortKey(sortAttribute));

        VirtualListViewRequestControl vlvRequest = new VirtualListViewRequestControl(this.offset, 0, this.limit - 1, contentCount, null);
        searchRequest.setControls(new Control[]{sortRequest, vlvRequest});
    }

    @Override
    public void createPage(SearchResult searchResult, int offset, int limit) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

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
    public List<SearchResultEntry> searchResultEntries() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public DefaultPaginator<T> limit(int limit) {
        if (limit < 1) {
            this.limit = config.getInt("ldap.paging.limit.default");
        } else if (limit >= config.getInt("ldap.paging.limit.max")) {
            this.limit = config.getInt("ldap.paging.limit.max");
        } else {
            this.limit = limit;
        }
        return this;
    }

    @Override
    public int limit() {
        return this.limit;
    }

    @Override
    public DefaultPaginator<T> offset(int offset) {
        this.offset = offset < 1 ? 1 : offset;
        return this;
    }

    @Override
    public int offset() {
        return this.offset;
    }
}
