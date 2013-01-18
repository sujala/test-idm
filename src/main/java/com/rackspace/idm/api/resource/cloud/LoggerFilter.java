package com.rackspace.idm.api.resource.cloud;

import com.rackspace.idm.api.filter.MultiReadHttpServletRequest;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
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
        MultiReadHttpServletRequest requestWrapper = new MultiReadHttpServletRequest((HttpServletRequest)request);

        String path = ((HttpServletRequestWrapper) requestWrapper).getPathInfo();
        String method = ((HttpServletRequestWrapper) requestWrapper).getMethod();
        String host = ((HttpServletRequestWrapper) requestWrapper).getHeader("Host");
        String remoteHost = ((HttpServletRequestWrapper) requestWrapper).getRemoteHost();
        String userAgent = ((HttpServletRequestWrapper) requestWrapper).getHeader("User-Agent");
        String authToken = ((HttpServletRequestWrapper) requestWrapper).getHeader("X-Auth-Token");
        String basicAuth = ((HttpServletRequestWrapper) requestWrapper).getHeader("Authorization");
        InputStream stream = requestWrapper.getInputStream();
        String requestBody = IOUtils.toString(stream);
        String requestType = ((HttpServletRequestWrapper) requestWrapper).getHeader("Content-Type");
        Long startTime = new Date().getTime();

        StatusExposingServletResponse responseWrapper = new StatusExposingServletResponse((HttpServletResponse)response);

        chain.doFilter(requestWrapper, responseWrapper);
        int status = responseWrapper.getStatus();
        String responseBody = responseWrapper.getBody();
        String responseType = ((HttpServletRequestWrapper) requestWrapper).getHeader("Accept");

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
