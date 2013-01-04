package com.rackspace.idm.api.resource.cloud;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;

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
        String userAgent = ((HttpServletRequestWrapper) request).getHeader("User-Agent");
        String authToken = ((HttpServletRequestWrapper) request).getHeader("X-Auth-Token");

        analyticsLogger.log(authToken, host, userAgent, method, path);

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
}
