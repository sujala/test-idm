package com.rackspace.idm.api.resource.pagination;

import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.exception.BadRequestException;
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
import org.springframework.test.context.ContextConfiguration;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.powermock.api.mockito.PowerMockito.doReturn;
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
    @Mock
    private UriInfo uriInfo;
    @InjectMocks
    DefaultPaginator<User> userPaginator = new DefaultPaginator<User>();

    private int contentCount = 0;
    private int offset = 0;
    private int limit = 10;

    private String sortAttribute = "uid";

    @Before
    public void setup() {
        when(uriInfo.getAbsolutePath()).thenReturn(makeURI());
        when(config.getInt("ldap.paging.limit.default")).thenReturn(25);
        when(config.getInt("ldap.paging.limit.max")).thenReturn(100);
    }

    @Test
    public void createSearchRequest_returnsContext() throws Exception {
        SearchRequest searchRequest = makeSearchRequest();

        PaginatorContext<User> context = userPaginator.createSearchRequest(sortAttribute, searchRequest, offset, limit);

        assertThat("context offset", context.getOffset(), equalTo(0));
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

        PaginatorContext<User> context = setupContext(0, 10, 0);
        context.setOffset(0);
        context.setLimit(10);

        userPaginator.createPage(searchResult, context);

        assertThat("resultSet", context.getTotalRecords(), equalTo(10));
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

        PaginatorContext<User> context = setupContext(0, 10, 5);
        userPaginator.createPage(searchResult, context);

        assertThat("context result set", context.getSearchResultEntryList().size(), equalTo(0));
        assertThat("context pageLinks", context.getTotalRecords(), equalTo(0));
    }

    @Test (expected = BadRequestException.class)
    public void formatLinkHeader_throwsBadRequest_offsetOutOfBounds() {
        PaginatorContext<User> context = setupContext(100, 10, 25);

        userPaginator.createLinkHeader(uriInfo, context);
    }

    @Test
    public void createLinkHeader_returnsLinkHeader() throws Exception {
        PaginatorContext<User> context = setupContext(30, 10, 100);
        String[] compareToHeader = {
                "<http://path.to.resource/this?marker=0&limit=10>; rel=\"first\"",
                "<http://path.to.resource/this?marker=20&limit=10>; rel=\"prev\"",
                "<http://path.to.resource/this?marker=40&limit=10>; rel=\"next\"",
                "<http://path.to.resource/this?marker=90&limit=10>; rel=\"last\""
        };

        String[] header = userPaginator.createLinkHeader(uriInfo, context).split(", ");
        assertThat("headers", header[0], equalTo(compareToHeader[0]));
        assertThat("headers", header[1], equalTo(compareToHeader[1]));
        assertThat("headers", header[2], equalTo(compareToHeader[2]));
        assertThat("headers", header[3], equalTo(compareToHeader[3]));
    }

    @Test
    public void createLinkHeader_returnsNull() throws Exception {
        PaginatorContext<User> context = new PaginatorContext<User>();

        String header = userPaginator.createLinkHeader(uriInfo, context);

        assertThat("header", header, equalTo(null));
    }

    @Test
    public void createLinkheader_onFirstPage_firstPageEqualsPrevPage() throws Exception {
        PaginatorContext<User> context = setupContext(10, 10, 100);

        String header = userPaginator.createLinkHeader(uriInfo, context);

        String[] links = header.split(", ");
        String prevLink = links[1].split(";")[0];
        String firstLink = links[0].split(";")[0];
        assertThat("prev == first", prevLink, equalToIgnoringCase(firstLink));
    }

    @Test
    public void createLinkHeader_onLastPage_lastPageEqualsNextPage() throws Exception {
        PaginatorContext<User> context = setupContext(10, 90, 100);

        String header = userPaginator.createLinkHeader(uriInfo, context);

        String[] links = header.split(", ");
        String nextLink = links[2].split(";")[0];
        String lasttLink = links[3].split(";")[0];
        assertThat("next == last", nextLink, equalToIgnoringCase(lasttLink));
    }

    @Test
    public void addNextLink_onLastpage_setsNextLinkToLast() throws Exception {
        StringBuilder builder = new StringBuilder();

        userPaginator.addNextLink(builder, uriInfo.getAbsolutePath().toString(), 30, 10, 40, 30);

        String compareTo = String.format("<%s?marker=%s&limit=%s>; rel=\"next\"",
                                            uriInfo.getAbsolutePath().toString(), 30, 10);

        assertThat("links match", builder.toString(), equalTo(compareTo));
    }

    @Test
    public void addNextLink_setsLink() throws Exception {
        StringBuilder builder = new StringBuilder();

        userPaginator.addNextLink(builder, uriInfo.getAbsolutePath().toString(), 10, 10, 40, 30);

        String compareTo = String.format("<%s?marker=%s&limit=%s>; rel=\"next\"",
                                            uriInfo.getAbsolutePath().toString(), 20, 10);

        assertThat("links match", builder.toString(), equalTo(compareTo));
    }

    @Test
    public void addPrevLink_onFirstPage_setsPrevLinkToFirst() throws Exception {
        StringBuilder builder = new StringBuilder();

        userPaginator.addPrevLink(builder, uriInfo.getAbsolutePath().toString(), 10, 10);

        String compareTo = String.format("<%s?marker=%s&limit=%s>; rel=\"prev\"",
                uriInfo.getAbsolutePath().toString(), 0, 10);

        assertThat("links match", builder.toString(), equalTo(compareTo));
    }

    @Test
    public void addPrevLink_setsLink() throws Exception {
        StringBuilder builder = new StringBuilder();

        userPaginator.addPrevLink(builder, uriInfo.getAbsolutePath().toString(), 30, 10);

        String compareTo = String.format("<%s?marker=%s&limit=%s>; rel=\"prev\"",
                uriInfo.getAbsolutePath().toString(), 20, 10);

        assertThat("links match", builder.toString(), equalTo(compareTo));
    }

    @Test
    public void addComma_addsComma() {
        StringBuilder builder = new StringBuilder();
        builder.append("this is the first string");

        userPaginator.addComma(builder);

        assertThat("builder has comma", builder.toString().indexOf(","), greaterThan(0));
    }

    @Test
    public void addComma_doesNotAddComma() {
        StringBuilder builder = new StringBuilder();

        userPaginator.addComma(builder);

        assertThat("builder has no comma", builder.toString().indexOf(","), equalTo(-1));
    }

    @Test
    public void makeLink_createsLink() {
        String link = userPaginator.makeLink("path", "?query", "first");
        String compareTo = "<path?query>; rel=\"first\"";
        assertThat("link created", link, equalTo(compareTo));
    }

    @Test
    public void withinFirstPage_isTrue() {
        boolean value = userPaginator.withinFirstPage(10, 10);
        assertThat("on first page", value, equalTo(true));
    }

    @Test
    public void withinFirstPage_isFalse() {
        boolean value = userPaginator.withinFirstPage(20, 10);
        assertThat("on first page", value, equalTo(false));
    }

    @Test
    public void withinLastPage_isTrue() {
        boolean value = userPaginator.withinLastPage(20, 10, 30);
        assertThat("on first page", value, equalTo(true));
    }

    @Test
    public void withinLastPage_isFalse() {
        boolean value = userPaginator.withinLastPage(20, 10, 40);
        assertThat("on first page", value, equalTo(false));
    }

    protected VirtualListViewRequestControl makeVLVRequestControl() {
        return new VirtualListViewRequestControl(offset, 0, limit, contentCount, null);
    }

    protected ServerSideSortRequestControl makeSortRequestControl() {
        return new ServerSideSortRequestControl(new SortKey("uid"));
    }

    protected SearchRequest makeSearchRequest() {
        Filter searchFilter = Filter.createEqualityFilter("uid", "1");
        return new SearchRequest(makeDN().toString(), SearchScope.SUB, searchFilter, "*");
    }

    protected SearchResultEntry makeResultEntry() {
        ArrayList<Attribute> attributeArrayList = new ArrayList<Attribute>();
        Attribute attribute = new Attribute("uid", "001");
        attributeArrayList.add(attribute);
        attributeArrayList.add(attribute);

        return new SearchResultEntry(makeDN().toString(), attributeArrayList, makeVLVRequestControl(), makeSortRequestControl());
    }

    protected VirtualListViewResponseControl makeVLVResponseControl() {
        return new VirtualListViewResponseControl(0, 10, ResultCode.SUCCESS, new ASN1OctetString("5"));
    }

    protected DN makeDN() {
        RDN rdn = new RDN("clientId", "abcd12345");
        RDN rdn1 = new RDN("cn", "DIRECT TOKENS");
        RDN rdn2 = new RDN("rsId", "123456789");
        RDN rdn3 = new RDN("ou", "users");
        RDN rdn4 = new RDN("o", "rackspace");
        RDN rdn5 = new RDN("dc", "rackspace");
        return new DN(rdn, rdn1, rdn2, rdn3, rdn4, rdn5);
    }

    protected URI makeURI() {
        try {
            return new URI("http://path.to.resource/this");
        } catch (URISyntaxException e) {
            return null;
        }
    }

    protected PaginatorContext<User> setupContext(int offset, int limit, int totalRecords) {
        PaginatorContext<User> context = new PaginatorContext<User>();
        context.setLimit(limit);
        context.setOffset(offset);
        context.setTotalRecords(totalRecords);
        return context;
    }
}
