package com.rackspace.idm.api.resource.cloud.migration;

import org.junit.Before;
import org.junit.Test;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 7/5/12
 * Time: 11:36 AM
 * To change this template use File | Settings | File Templates.
 */
public class MigrationEndpointListTest {
    private  MigrationEndpointList migrationEndpointList;

    @Before
    public void setUp() throws Exception {
        migrationEndpointList = new MigrationEndpointList();
    }

    @Test
    public void getEndpoint_endpointIsNull_returnsEmptyList() throws Exception {
        List<MigrationEndpoint> result = migrationEndpointList.getEndpoint();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void getEndpoint_endpointExists_returnsEndpointList() throws Exception {
        List<MigrationEndpoint> endpoint = migrationEndpointList.getEndpoint();
        MigrationEndpoint migrationEndpoint = new MigrationEndpoint();
        endpoint.add(migrationEndpoint);
        List<MigrationEndpoint> result = migrationEndpointList.getEndpoint();
        assertThat("migration endpoint", result.get(0), equalTo(migrationEndpoint));
    }

    @Test
    public void getLink_linkIsNull_returnsEmptyList() throws Exception {
        List<String> result = migrationEndpointList.getLink();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void getLink_linkExists_returnsList() throws Exception {
        List<String> link = migrationEndpointList.getLink();
        link.add("test");
        List<String> result = migrationEndpointList.getLink();
        assertThat("list", result.get(0), equalTo("test"));
    }

    @Test
    public void getAny_anyIsNull_returnsEmptyList() throws Exception {
        List<Object> result = migrationEndpointList.getAny();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void getAny_anyExists_returnsList() throws Exception {
        List<Object> list = migrationEndpointList.getAny();
        list.add("test");
        List<Object> result = migrationEndpointList.getAny();
        assertThat("list", result.get(0).toString(), equalTo("test"));
    }

    @Test
    public void getOtherAttributes_returnsOtherAttributes() throws Exception {
        Map<QName,String> result = migrationEndpointList.getOtherAttributes();
        assertThat("hash map", result.isEmpty(), equalTo(true));
    }
}
