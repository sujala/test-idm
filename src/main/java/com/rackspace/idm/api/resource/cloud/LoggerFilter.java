package com.rackspace.idm.api.resource.cloud;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

@Component
public class LoggerFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        ServletContext servletContext = filterConfig.getServletContext();
        WebApplicationContext webApplicationContext = WebApplicationContextUtils.getWebApplicationContext(servletContext);

        AutowireCapableBeanFactory autowireCapableBeanFactory = webApplicationContext.getAutowireCapableBeanFactory();

        autowireCapableBeanFactory.configureBean(this, "analyticsLogger");
    }

    @Autowired
    AnalyticsLogger analyticsLogger;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        String path = ((HttpServletRequestWrapper) request).getPathInfo();
        String method = ((HttpServletRequestWrapper) request).getMethod();
        String host = ((HttpServletRequestWrapper) request).getHeader("Host");
        String remoteHost = ((HttpServletRequestWrapper) request).getRemoteHost();
        String userAgent = ((HttpServletRequestWrapper) request).getHeader("User-Agent");
        String authToken = ((HttpServletRequestWrapper) request).getHeader("X-Auth-Token");
        String basicAuth = ((HttpServletRequestWrapper) request).getHeader("Authorization");
        InputStream stream = request.getInputStream();
        String requestBody = IOUtils.toString(stream);
        String requestType = ((HttpServletRequestWrapper) request).getHeader("Content-Type");
        Long startTime = new Date().getTime();

        StatusExposingServletResponse responseWrapper = new StatusExposingServletResponse((HttpServletResponse)response);

        chain.doFilter(request, responseWrapper);
        int status = responseWrapper.getStatus();
        String responseBody = responseWrapper.getBody();
        String responseType = ((HttpServletRequestWrapper) request).getHeader("Accept");

        // TODO: make async
        analyticsLogger.log
                (
                        startTime,
                        authToken,
                        basicAuth,
                        host,
                        remoteHost,
                        userAgent,
                        method,
                        path,
                        status,
                        requestBody,
                        requestType,
                        responseBody,
                        responseType
                );
    }

    @Override
    public void destroy() {
    }
}
