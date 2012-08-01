package com.rackspace.idm.api.resource.cloud;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public final class WebClientDevWrapper {

    private WebClientDevWrapper() {}

    private static Logger logger = LoggerFactory.getLogger(WebClientDevWrapper.class);

    public static HttpClient wrapClient(HttpClient base) {
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            X509TrustManager tm = new X509TrustManager() {

                @Override
                public void checkClientTrusted(X509Certificate[] chain,
                                               String authType) throws CertificateException {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain,
                                               String authType) throws CertificateException {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };
            ctx.init(null, new TrustManager[]{tm}, null);
            SSLSocketFactory ssf = new SSLSocketFactory(ctx);
            ssf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            ClientConnectionManager ccm = base.getConnectionManager();
            ccm.getSchemeRegistry().register(new Scheme("https", ssf, 443));
            //DefaultHttpClient defaultHttpClient = new DefaultHttpClient(ccm, base.getParams());
            //defaultHttpClient.addRequestInterceptor(new RequestAcceptEncoding());
            //defaultHttpClient.addResponseInterceptor(new ResponseContentEncoding());
            return base;
        } catch (Exception ex) {
            logger.info("failed to wrap HttpClient " + ex.getMessage());
            return null;
        }
    }
}
