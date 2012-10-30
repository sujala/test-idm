package com.rackspace.idm.api.resource.pagination;

import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.api.resource.pagination.*;
import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.ldap.sdk.*;
import com.unboundid.ldap.sdk.controls.ServerSideSortRequestControl;
import com.unboundid.ldap.sdk.controls.SortKey;
import com.unboundid.ldap.sdk.controls.VirtualListViewRequestControl;
import com.unboundid.ldap.sdk.controls.VirtualListViewResponseControl;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Created with IntelliJ IDEA.
 * User: jacob
 * Date: 10/10/12
 * Time: 5:24 PM
 * To change this template use File | Settings | File Templates.
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest(VirtualListViewResponseControl.class)
@ContextConfiguration(locations = "classpath:app-config.xml")
public class DefaultPaginatorTest {

    @Mock
    private Configuration config;
    @InjectMocks
    DefaultPaginator<User> userPaginator = new DefaultPaginator<User>();

    private int contentCount = 0;
    private int offset = 0;
    private int limit = 10;

    private String sortAttribute = "uid";

    @Before
    public void setup() {
        when(config.getInt("ldap.paging.limit.default")).thenReturn(25);
        when(config.getInt("ldap.paging.limit.max")).thenReturn(100);
    }

    @Test
    public void createSearchRequest_setsOffset_default() throws Exception {
        SearchRequest searchRequest = makeSearchRequest();

        PaginatorContext<User> context = userPaginator.createSearchRequest(sortAttribute, searchRequest, -5, 10);

        assertThat("offset", context.getOffset(), equalTo(1));
    }

    @Test
    public void createSearchRequest_setsLimit_defaultMax() throws Exception {
        SearchRequest searchRequest = makeSearchRequest();

        PaginatorContext<User> context = userPaginator.createSearchRequest(sortAttribute, searchRequest, offset, 1000000);

        assertThat("context limit", context.getLimit(), equalTo(100));
    }

    @Test
    public void createSearchRequest_setsLimit_default() throws Exception {
        SearchRequest searchRequest = makeSearchRequest();

        PaginatorContext<User> context = userPaginator.createSearchRequest(sortAttribute, searchRequest, offset, -100);

        assertThat("context limit", context.getLimit(), equalTo(25));
    }

    @Test
    public void createSearchRequest_setsControls() throws Exception {
        SearchRequest searchRequest = makeSearchRequest();
        SearchRequest compareRequest = makeSearchRequest();

        VirtualListViewRequestControl vlvControlForCompare = new VirtualListViewRequestControl(1, 0, limit - 1, contentCount, null);
        ServerSideSortRequestControl sortRequest = makeSortRequestControl();
        compareRequest.setControls(sortRequest, vlvControlForCompare);

        userPaginator.createSearchRequest(sortAttribute, searchRequest, offset, limit);

        assert(searchRequest.equals(compareRequest));
    }

    @Test
    public void createSearchRequest_returnsContext() throws Exception {
        SearchRequest searchRequest = makeSearchRequest();

        PaginatorContext<User> context = userPaginator.createSearchRequest(sortAttribute, searchRequest, offset, limit);

        assertThat("context offset", context.getOffset(), equalTo(1));
        assertThat("context limit", context.getLimit(), equalTo(10));
    }

    @Test
    public void createPage_createsPageLinks() throws Exception {
        List<SearchResultEntry> resultList = new ArrayList<SearchResultEntry>();
        List<SearchResultReference> resultReferenceList = new ArrayList<SearchResultReference>();
        VirtualListViewRequestControl vlvControl = makeVLVRequestControl();
        ServerSideSortRequestControl sortRequest = makeSortRequestControl();

        SearchResultEntry resultEntry = makeResultEntry();
        SearchResultReference resultReference = new SearchResultReference(new String[]{}, new Control[]{vlvControl, sortRequest});
        for (int i = 0; i < 5; i++) {
            resultList.add(resultEntry);
            resultReferenceList.add(resultReference);
        }

        VirtualListViewResponseControl responseControl = makeVLVResponseControl();

        SearchResult searchResult = new SearchResult(0, ResultCode.SUCCESS, null, "baseDN", null,
                resultList, resultReferenceList, 5, 5, new Control[]{sortRequest, vlvControl});

        mockStatic(VirtualListViewResponseControl.class);
        when(VirtualListViewResponseControl.get(searchResult)).thenReturn(responseControl);

        PaginatorContext<User> context = new PaginatorContext<User>();
        context.setOffset(1);
        context.setLimit(10, 10, 100);

        userPaginator.createPage(searchResult, context);

        assertThat("resultSet", context.getPageLinks().size(), equalTo(4));
    }

    @Test
    public void createPage_returnsContext_withEmptyResultSet() throws Exception {
        List<SearchResultEntry> resultList = new ArrayList<SearchResultEntry>();
        List<SearchResultReference> resultReferenceList = new ArrayList<SearchResultReference>();
        VirtualListViewRequestControl vlvControl = makeVLVRequestControl();
        ServerSideSortRequestControl sortRequest = makeSortRequestControl();

        SearchResult searchResult = new SearchResult(0, ResultCode.SUCCESS, null, "baseDN", null,
                resultList, resultReferenceList, 0, 0, new Control[]{sortRequest, vlvControl});
        mockStatic(VirtualListViewResponseControl.class);
        when(VirtualListViewResponseControl.get(searchResult)).thenThrow(new LDAPException(ResultCode.FILTER_ERROR));

        PaginatorContext<User> context = new PaginatorContext<User>();
        userPaginator.createPage(searchResult, context);

        assertThat("context result set", context.getSearchResultEntryList().size(), equalTo(0));
    }

    protected VirtualListViewRequestControl makeVLVRequestControl() {
        return new VirtualListViewRequestControl(offset, 0, limit, contentCount, null);
    }

    protected ServerSideSortRequestControl makeSortRequestControl() {
        return new ServerSideSortRequestControl(new SortKey("uid"));
    }

    protected SearchRequest makeSearchRequest() {
        Filter searchFilter = Filter.createEqualityFilter("uid", "1");
        return new SearchRequest("baseDN", SearchScope.SUB, searchFilter, "*");
    }

    protected SearchResultEntry makeResultEntry() {
        ArrayList<Attribute> attributeArrayList = new ArrayList<Attribute>();
        Attribute attribute = new Attribute("uid", "001");
        attributeArrayList.add(attribute);
        attributeArrayList.add(attribute);

        return new SearchResultEntry("baseDN", attributeArrayList, makeVLVRequestControl(), makeSortRequestControl());
    }

    protected VirtualListViewResponseControl makeVLVResponseControl() {
        return new VirtualListViewResponseControl(0, 10, ResultCode.SUCCESS, new ASN1OctetString("5"));
    }
}
