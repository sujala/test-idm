package com.rackspace.idm.api.filter;

import com.rackspace.idm.exception.BadRequestException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class SecurityFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        MultiReadHttpServletRequest requestWrapper = new MultiReadHttpServletRequest((HttpServletRequest)request);
        InputStream stream = requestWrapper.getInputStream();
        String entityString = IOUtils.toString(stream);

        if (StringUtils.containsIgnoreCase(entityString, "!DOCTYPE") || StringUtils.containsIgnoreCase(entityString, "!ENTITY")) {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid request");
        }

        chain.doFilter(requestWrapper, response);
    }

    @Override
    public void destroy() {
    }
}

