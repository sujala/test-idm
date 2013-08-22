package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.PaginatorContext;
import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;

public interface PaginatorDao<T> {

    PaginatorContext<T> createSearchRequest(String sortAttribute, SearchRequest searchRequest, int offset, int limit);

    PaginatorContext<T> createSearchRequest(String sortAttribute, SearchRequest searchRequest, ASN1OctetString contextId, int contentCount, int offset, int limit);

    void createPage(SearchResult searchResult, PaginatorContext<T> context);
}
