package com.rackspace.idm.api.resource.cloud;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class UrlFilter implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(UrlFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        final HttpServletRequest req = (HttpServletRequest) request;

        final String uri = req.getRequestURI();
        final String decodeUri = urlDecode(uri);
        final String pathInfo = req.getPathInfo();
        final String decodePathInfo = urlDecode(pathInfo);
        final Map decodeMap = new HashMap();
        final Map<String, String[]> map = req.getParameterMap();
        if (!map.isEmpty()) {
            Iterator iterator = map.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry mapEntry = (Map.Entry) iterator.next();
                String[] values = (String[]) mapEntry.getValue();
                String decode = urlDecode(values[0]);
                decodeMap.put(mapEntry.getKey(), decode);
            }
        }

        final HttpServletRequestWrapper newReq = new HttpServletRequestWrapper(req) {
            public String getRequestURI() {
                return decodeUri;
            }

            public String getPathInfo() {
                return decodePathInfo;
            }

            public Map getParameterMap() {
                if (!map.isEmpty()) {
                    return decodeMap;
                } else {
                    return req.getParameterMap();
                }

            }
        };
        chain.doFilter(newReq, response);
    }

    @Override
    public void destroy() {

    }

    private String urlDecode(String value) {
        try {
            value = URLDecoder.decode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            LOG.warn("UTF-8 encoding can not be used to decode " + value);
        }
        return value;
    }
}
