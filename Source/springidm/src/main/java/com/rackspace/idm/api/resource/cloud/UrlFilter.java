package com.rackspace.idm.api.resource.cloud;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.cxf.common.util.UrlUtils;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: Mar 20, 2012
 * Time: 12:52:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class UrlFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        final HttpServletRequest req = (HttpServletRequest) request;

        final String uri = req.getRequestURI();
        final String decodeUri = UrlUtils.urlDecode(uri);
        final String pathInfo = req.getPathInfo();
        final String decodePathInfo = UrlUtils.urlDecode(pathInfo);
        Map<String,String[]> map = req.getParameterMap();
        final Map decodeMap = new HashMap();
        Iterator iterator = map.entrySet().iterator();
        while(iterator.hasNext()){
            Map.Entry mapEntry = (Map.Entry)iterator.next();
            String[] values = (String[])mapEntry.getValue();
            String decode = UrlUtils.urlDecode(values[0]);
            decodeMap.put(mapEntry.getKey(),decode);
        }

        final HttpServletRequestWrapper newReq = new HttpServletRequestWrapper(req){
            public String getRequestURI(){
                return decodeUri;
            }
            public String getPathInfo(){
                return decodePathInfo;
            }
            public Map getParameterMap(){
                return decodeMap;
            }
        };

        chain.doFilter(newReq,response);
    }

    @Override
    public void destroy() {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
