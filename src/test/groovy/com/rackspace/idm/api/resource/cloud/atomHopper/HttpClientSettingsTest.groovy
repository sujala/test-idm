package com.rackspace.idm.api.resource.cloud.atomHopper

import com.rackspace.idm.domain.config.IdentityConfig
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.Configurable
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.impl.client.InternalHttpClient
import org.apache.http.impl.conn.PoolingClientConnectionManager
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import spock.lang.Shared
import spock.lang.Specification

/**
 * Purpose of this test to to validate that the exposed HttpClient properties are appropriately set on the httpclient
 * per https://one.rackspace.com/display/auth/Identity+HttpClient+for+Cloud+Feeds+Event+Posting
 */
class HttpClientSettingsTest extends Specification {
    @Shared AtomHopperClient client

    @Shared AtomHopperHelper atomHopperHelper

    @Shared IdentityConfig identityConfig
    @Shared IdentityConfig.StaticConfig staticConfig
    @Shared IdentityConfig.ReloadableConfig reloadableConfig

    def setup() {
        client = new AtomHopperClient();

        atomHopperHelper = Mock()
        client.atomHopperHelper = atomHopperHelper

        identityConfig = Mock()
        staticConfig = Mock()
        reloadableConfig = Mock()
        identityConfig.getStaticConfig() >> staticConfig
        identityConfig.getReloadableConfig() >> reloadableConfig
        client.identityConfig = identityConfig

        reloadableConfig.getAtomHopperDataCenter() >> "GLOBAL"
        reloadableConfig.getAtomHopperUrl() >> "http://localhost:8888/namespace/feed"
        reloadableConfig.getAtomHopperRegion() >> "GLOBAL"
    }

    def cleanup() {
        client.destroy()
    }

    def "init can create config legacy http client"() {
        staticConfig.useFeedsConfigurableHttpClient() >> false
        staticConfig.getFeedsMaxTotalConnections() >> 14
        staticConfig.getFeedsMaxConnectionsPerRoute() >> 3

        when: "use legacy client"
        client.init()
        CloseableHttpClient httpClient = client.httpClient

        then:
        httpClient != null
        httpClient instanceof DefaultHttpClient

        and: "maxes use hardcoded rather than configurable"
        httpClient.connectionManager != null
        httpClient.connectionManager instanceof PoolingClientConnectionManager
        PoolingClientConnectionManager pcm = httpClient.connectionManager
        pcm.getMaxTotal() == 200
        pcm.getDefaultMaxPerRoute() == 200
    }

    def "init can create configurable http client"() {
        staticConfig.useFeedsConfigurableHttpClient() >> true
        staticConfig.getFeedsMaxTotalConnections() >> 12
        staticConfig.getFeedsMaxConnectionsPerRoute() >> 5
        staticConfig.getFeedsOnUseEvictionValidateAfterInactivity() >> 4
        staticConfig.getFeedsNewConnectionSocketTimeout() >> 1250

        reloadableConfig.getFeedsConnectionRequestTimeout() >> 1111
        reloadableConfig.getFeedsConnectionTimeout() >> 2222
        reloadableConfig.getFeedsSocketTimeout() >> 3333

        when: "use configurable client"
        client.init()
        CloseableHttpClient httpClient = client.httpClient

        then:
        assert httpClient != null
        !(httpClient instanceof DefaultHttpClient) //uses builder so exact type is impl dependant

        and: "pool is properly configured"
        PoolingHttpClientConnectionManager pcm = (PoolingHttpClientConnectionManager) httpClient.connManager
        pcm instanceof PoolingHttpClientConnectionManager
        pcm.getMaxTotal() == 12
        pcm.getDefaultMaxPerRoute() == 5
        pcm.getValidateAfterInactivity() == 4
        pcm.getDefaultSocketConfig().getSoTimeout() == 1250

        and: "default request config is configured"
        RequestConfig reqConfig = ((Configurable)httpClient).getConfig()
        reqConfig.connectionRequestTimeout == 1111
        reqConfig.connectTimeout == 2222
        reqConfig.socketTimeout == 3333
    }
}
