package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.entity.PaginatorContext;
import com.rackspace.idm.domain.dao.GenericDao;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.SearchScope;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PagingIterator<T> extends ArrayList<T> implements Iterable<T>,List<T> {

    private static final int PAGE_SIZE = 1000;
    private int offset;
    private int index;

    private PaginatorContext<T> context;
    private GenericDao<T> repo;
    private Filter searchFilter;
    private String dn;
    private SearchScope scope;

    public PagingIterator(GenericDao<T> repo, Filter searchFilter, String dn, SearchScope scope) {
        this.repo = repo;
        this.searchFilter = searchFilter;
        this.dn = dn;
        this.scope = scope;
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
                    context = repo.getObjectsPaged(searchFilter, dn, scope, offset, PAGE_SIZE);
                    offset += PAGE_SIZE;
                    index = 0;
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
