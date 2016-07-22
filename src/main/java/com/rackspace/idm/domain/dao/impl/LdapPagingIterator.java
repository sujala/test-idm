package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.dao.GenericDao;
import com.rackspace.idm.domain.entity.PaginatorContext;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.SearchScope;

import java.util.Iterator;

public class LdapPagingIterator<T> implements Iterable<T> {

    private int offset;
    private int index;

    private PaginatorContext<T> context;
    private GenericDao<T> repo;
    private Filter searchFilter;
    private String dn;
    private SearchScope scope;

    private final int PAGE_SIZE;

    public LdapPagingIterator(GenericDao<T> repo, Filter searchFilter, String dn, SearchScope scope, int pageSize) {
        this.repo = repo;
        this.searchFilter = searchFilter;
        this.dn = dn;
        this.scope = scope;
        this.PAGE_SIZE = pageSize;
    }

    @Override
    public Iterator<T> iterator() {
        Iterator<T> it = new Iterator<T>() {
            @Override
            public boolean hasNext() {
                if (context == null) {
                    context = repo.getObjectsPaged(searchFilter, dn, scope, offset, PAGE_SIZE);
                    offset = 0;
                    index = 0;
                }
                return offset + index < context.getTotalRecords();
            }

            @Override
            public T next() {
                if (index >= PAGE_SIZE) {
                    offset += PAGE_SIZE;
                    index = 0;
                    context = repo.getObjectsPaged(searchFilter, dn, scope, offset, PAGE_SIZE);
                }
                return context.getValueList().get(index++);
            }

            @Override
            public void remove() {
            }
        };
        return it;
    }
}
