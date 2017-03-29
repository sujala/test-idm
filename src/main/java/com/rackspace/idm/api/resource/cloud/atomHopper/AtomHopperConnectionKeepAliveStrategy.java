package com.rackspace.idm.api.resource.cloud.atomHopper;

import com.rackspace.idm.domain.config.IdentityConfig;
import org.apache.http.HttpResponse;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.protocol.HttpContext;

public class AtomHopperConnectionKeepAliveStrategy implements ConnectionKeepAliveStrategy {

    DefaultConnectionKeepAliveStrategy connectionKeepAliveStrategy = new DefaultConnectionKeepAliveStrategy();
    IdentityConfig identityConfig;

    public AtomHopperConnectionKeepAliveStrategy(IdentityConfig identityConfig) {
        this.identityConfig = identityConfig;
    }

     /**
     * Returns the duration of time which this connection can be safely kept
     * idle. If the connection is left idle for longer than this period of time,
     * it MUST not reused. A value of 0 or less may be returned to indicate that
     * there is no suitable suggestion.
     *
     * When coupled with a {@link org.apache.http.ConnectionReuseStrategy}, if
     * {@link org.apache.http.ConnectionReuseStrategy#keepAlive(
     *   HttpResponse, HttpContext)} returns true, this allows you to control
     * how long the reuse will last. If keepAlive returns false, this should
     * have no meaningful impact
     *
     * @param response
     *            The last response received over the connection.
     * @param context
     *            the context in which the connection is being used.
     *
     * @return the duration in ms for which it is safe to keep the connection
     *         idle, or &lt;=0 if no suggested duration.
     */

    @Override
    public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
        long duration = connectionKeepAliveStrategy.getKeepAliveDuration(response, context);
        if (duration == -1 && identityConfig.getReloadableConfig().getFeedsAllowConnectionKeepAlive()) {
            // Connection didn't specify a keep-alive, so use configured length rather than assume can be reused forever
            duration = identityConfig.getReloadableConfig().getFeedsConnectionKeepAliveDuration();
        }
        return duration;
    }
}
