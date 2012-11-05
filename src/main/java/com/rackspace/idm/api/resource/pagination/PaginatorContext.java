package com.rackspace.idm.api.resource.pagination;

import com.rackspace.idm.exception.BadRequestException;
import com.unboundid.ldap.sdk.SearchResultEntry;
import lombok.Data;

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
@Data
public class PaginatorContext<T> {
    private List<SearchResultEntry> searchResultEntryList;
    private List<T> valueList;

    private int offset;
    private int limit;
    private int totalRecords;


    public List<SearchResultEntry> getSearchResultEntryList() {
        if (searchResultEntryList == null) {
            searchResultEntryList = new ArrayList<SearchResultEntry>();
        }
        return searchResultEntryList;
    }

    public List<T> getValueList() {
        if (valueList == null) {
            valueList = new ArrayList<T>();
        }
        return valueList;
    }
}
