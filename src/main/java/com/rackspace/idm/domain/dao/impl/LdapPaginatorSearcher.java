package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.dao.PaginatorDao;
import com.rackspace.idm.domain.entity.PaginatorContext;
import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.controls.ServerSideSortRequestControl;
import com.unboundid.ldap.sdk.controls.SortKey;
import com.unboundid.ldap.sdk.controls.VirtualListViewRequestControl;
import com.unboundid.ldap.sdk.controls.VirtualListViewResponseControl;
import org.springframework.stereotype.Component;

@Component
public class LdapPaginatorSearcher<T> implements PaginatorDao <T>{

    @Override
    public PaginatorContext<T> createSearchRequest(String sortAttribute, SearchRequest searchRequest, int offset, int limit) {
        return createSearchRequest(sortAttribute, searchRequest, null, 0, offset, limit);
    }

    @Override
    public PaginatorContext<T> createSearchRequest(String sortAttribute, SearchRequest searchRequest, ASN1OctetString contextId, int contentCount, int offset, int limit) {
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
}
