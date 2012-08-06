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
 * Time: 10:08 AM
 * To change this template use File | Settings | File Templates.
 */
public class MigrationEndpointTest {
    private MigrationEndpoint migrationEndpoint;

    @Before
    public void setUp() throws Exception {
        migrationEndpoint = new MigrationEndpoint();

        migrationEndpoint.setAdminURL("adminUrl");
        migrationEndpoint.setComment("comment");
        migrationEndpoint.setId(1);
        migrationEndpoint.setInternalURL("internalUrl");
        migrationEndpoint.setName("name");
        migrationEndpoint.setPublicURL("publicUrl");
        migrationEndpoint.setRegion("region");
        migrationEndpoint.setTenantId("tenantId");
        migrationEndpoint.setType("type");
        migrationEndpoint.setValid(true);
        migrationEndpoint.setVersion("version");
    }

    @Test
    public void getVersion_returnsVersion() throws Exception {
        String result = migrationEndpoint.getVersion();
        assertThat("version", result, equalTo("version"));
    }

    @Test
    public void getLink_linkIsNull_createsEmptyStringList() throws Exception {
        List<String> result = migrationEndpoint.getLink();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void getLink_linkExists_returnsLink() throws Exception {
        List<String> link = migrationEndpoint.getLink();
        link.add("test");
        List<String> result = migrationEndpoint.getLink();
        assertThat("list", result.get(0), equalTo("test"));
    }

    @Test
    public void getAny_anyIsNull_createsEmptyObjectList() throws Exception {
        List<Object> result = migrationEndpoint.getAny();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void getAny_listExists_returnsList() throws Exception {
        List<Object> any = migrationEndpoint.getAny();
        any.add("test");
        List<Object> result = migrationEndpoint.getAny();
        assertThat("list", result.get(0).toString(), equalTo("test"));
    }

    @Test
    public void getComment_returnsComment() throws Exception {
        String result = migrationEndpoint.getComment();
        assertThat("comment", result, equalTo("comment"));
    }

    @Test
    public void isValid_returnsBooleanValue() throws Exception {
        Boolean result = migrationEndpoint.isValid();
        assertThat("boolean", result, equalTo(true));
    }

    @Test
    public void getId_returnsId() throws Exception {
        int result = migrationEndpoint.getId();
        assertThat("id", result, equalTo(1));
    }

    @Test
    public void getType_returnsType() throws Exception {
        String result = migrationEndpoint.getType();
        assertThat("type", result, equalTo("type"));
    }

    @Test
    public void getName_returnsName() throws Exception {
        String result = migrationEndpoint.getName();
        assertThat("name", result, equalTo("name"));
    }

    @Test
    public void getRegion_returnsRegion() throws Exception {
        String result = migrationEndpoint.getRegion();
        assertThat("region", result, equalTo("region"));
    }

    @Test
    public void getPublicURL_returnsPublicURL() throws Exception {
        String result = migrationEndpoint.getPublicURL();
        assertThat("public url", result, equalTo("publicUrl"));
    }

    @Test
    public void getInternalURL_returnsInternalURL() throws Exception {
        String result = migrationEndpoint.getInternalURL();
        assertThat("internal url", result, equalTo("internalUrl"));
    }

    @Test
    public void getAdminURL_returnsAdminURL() throws Exception {
        String result = migrationEndpoint.getAdminURL();
        assertThat("admin url", result, equalTo("adminUrl"));
    }

    @Test
    public void getTenantId_returnsTenantId() throws Exception {
        String result = migrationEndpoint.getTenantId();
        assertThat("tenant id", result, equalTo("tenantId"));
    }

    @Test
    public void getOtherAttributes_returnsOtherAttributes() throws Exception {
        Map<QName,String> result = migrationEndpoint.getOtherAttributes();
        assertThat("hash map", result.isEmpty(), equalTo(true));
    }
}
