package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.exception.IdmException;
import org.joda.time.DateTime;
import org.junit.Test;
import org.openstack.docs.identity.api.v2.Token;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 8/2/12
 * Time: 5:26 PM
 * To change this template use File | Settings | File Templates.
 */
public class JSONReaderForCloudAuthenticationResponseTokenTest {

    String testString = "{\"access\":{\"token\":{\"id\":\"1319b190-9527-46e7-9c0e-4fc3ca032e57\",\"expires\":\"2012-08-03T14:56:25.000-05:00\",\"tenant\":{\"id\":\"MossoCloudFS_6eee84c5-54a4-4217-a895-8308da81feb3\",\"name\":\"MossoCloudFS_6eee84c5-54a4-4217-a895-8308da81feb3\"}},\"serviceCatalog\":[{\"endpoints\":[{\"region\":\"STAGING\",\"tenantId\":\"-16\",\"publicURL\":\"https:\\/\\/cmstaging.api.rackspacecloud.com\\/v1.0\\/-16\"}],\"name\":\"cloudMonitoring\",\"type\":\"rax:monitor\"},{\"endpoints\":[{\"region\":\"STAGING\",\"tenantId\":\"-16\",\"publicURL\":\"https:\\/\\/staging.ord.loadbalancers.api.rackspacecloud.com\\/v1.0\\/-16\"}],\"name\":\"cloudLoadBalancers\",\"type\":\"rax:load-balancer\"},{\"endpoints\":[{\"tenantId\":\"-16\",\"publicURL\":\"https:\\/\\/staging.dnsaas.rackspace.net\\/v1.0\\/-16\"}],\"name\":\"cloudDNS\",\"type\":\"rax:dns\"},{\"endpoints\":[{\"tenantId\":\"-16\",\"publicURL\":\"https:\\/\\/servers.api.staging.us.ccp.rackspace.net\\/v1.0\\/-16\",\"versionInfo\":\"https:\\/\\/servers.api.staging.us.ccp.rackspace.net\\/v1.0\",\"versionList\":\"https:\\/\\/servers.api.staging.us.ccp.rackspace.net\\/v1.0\",\"versionId\":\"1.0\"}],\"name\":\"cloudServers\",\"type\":\"compute\"},{\"endpoints\":[{\"region\":\"STAGING\",\"tenantId\":\"-16\",\"publicURL\":\"https:\\/\\/api.staging.ord1.clouddb.rackspace.net\\/v1.0\\/-16\"}],\"name\":\"cloudDatabases\",\"type\":\"rax:database\"},{\"endpoints\":[{\"region\":\"ORD\",\"tenantId\":\"-16\",\"publicURL\":\"https:\\/\\/preprod.ord.servers.api.rackspacecloud.com\\/v2\\/-16\"},{\"region\":\"DFW\",\"tenantId\":\"-16\",\"publicURL\":\"https:\\/\\/preprod.dfw.servers.api.rackspacecloud.com\\/v2\\/-16\"}],\"name\":\"cloudServersPreprod\",\"type\":\"compute\"},{\"endpoints\":[{\"region\":\"ORD\",\"tenantId\":\"MossoCloudFS_6eee84c5-54a4-4217-a895-8308da81feb3\",\"publicURL\":\"https:\\/\\/cdn.stg.clouddrive.com\\/v1\\/MossoCloudFS_6eee84c5-54a4-4217-a895-8308da81feb3\"}],\"name\":\"cloudFilesCDN\",\"type\":\"rax:object-cdn\"},{\"endpoints\":[{\"region\":\"ORD\",\"tenantId\":\"MossoCloudFS_6eee84c5-54a4-4217-a895-8308da81feb3\",\"publicURL\":\"https:\\/\\/storage.stg.swift.racklabs.com\\/v1\\/MossoCloudFS_6eee84c5-54a4-4217-a895-8308da81feb3\",\"internalURL\":\"https:\\/\\/snet-storage.stg.swift.racklabs.com\\/v1\\/MossoCloudFS_6eee84c5-54a4-4217-a895-8308da81feb3\"}],\"name\":\"cloudFiles\",\"type\":\"object-store\"}],\"user\":{\"id\":\"153345\",\"roles\":[{\"id\":\"357\",\"description\":\"Customer Admin Access\",\"name\":\"customer:admin\"},{\"id\":\"1\",\"description\":\"Admin Role.\",\"name\":\"identity:admin\"}],\"name\":\"auth\",\"RAX-AUTH:defaultRegion\":\"\"}}}";

    @Test
    public void getAuthenticationResponseTokenFromJSONString_withCloudAuthResponse_returnsCorrectToken() throws Exception {
        Token token = JSONReaderForCloudAuthenticationResponseToken.getAuthenticationResponseTokenFromJSONString(testString);
        assertThat("token id", token.getId(), equalTo("1319b190-9527-46e7-9c0e-4fc3ca032e57"));
        assertThat("token expires", token.getExpires().toString(), equalTo("2012-08-03T14:56:25.000-05:00"));
    }

    @Test
    public void getAuthenticationResponseTokenFromJSONString_accessDoesNotContainToken_returnsToken() throws Exception {
        String jsonBody = "{\"access\":{}}";
        Token token = JSONReaderForCloudAuthenticationResponseToken.getAuthenticationResponseTokenFromJSONString(jsonBody);
        assertThat("token id", token.getId(), equalTo(null));
    }

    @Test
    public void getAuthenticationResponseTokenFromJSONString_tokenIdAndtokenExpiresIsNullAndTenantIsNull_doesNotSetIdAndExpiresAndTenant() throws Exception {
        String jsonBody = "{\"access\":{\"token\":{}}}";
        Token token = JSONReaderForCloudAuthenticationResponseToken.getAuthenticationResponseTokenFromJSONString(jsonBody);
        assertThat("token id", token.getId(), equalTo(null));
        assertThat("token expires", token.getExpires(), equalTo(null));
        assertThat("tenant", token.getTenant(), equalTo(null));
    }

    @Test
    public void getAuthenticationResponseTokenFromJSONString_tenantIdIsNullAndTenantNameIsNull_doesNotSetIdAndName() throws Exception {
        String jsonBody = "{\"access\":{\"token\":{\"tenant\":{}}}}";
        Token token = JSONReaderForCloudAuthenticationResponseToken.getAuthenticationResponseTokenFromJSONString(jsonBody);
        assertThat("tenant id", token.getTenant().getId(), equalTo(null));
        assertThat("tenant name", token.getTenant().getName(), equalTo(null));
    }

    @Test (expected = IdmException.class)
    public void getAuthenticationResponseTokenFromJSONString_unableToParseJsonString_throwsIdmException() throws Exception {
        String jsonBody = "";
        JSONReaderForCloudAuthenticationResponseToken.getAuthenticationResponseTokenFromJSONString(jsonBody);
    }
}
