package com.rackspace.idm.api.resource.cloud;

import com.rackspace.idm.api.filter.MultiReadHttpServletRequest;
import com.unboundid.util.Debug;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.*;
import javax.servlet.Filter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.logging.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class LoggerFilter implements Filter {
    private static org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(LoggerFilter.class);

    public static final String UNBOUNDID_LOG_LOCATION_PROP_NAME = "unboundid.log.location";
    public static final String UNBOUNDID_LOG_LOCATION_DEFAULT = "/var/log/idm/ldap.log";

    public static final String UNBOUNDID_LOG_ENABLE_PROP_NAME = "unboundid.log.enable";
    public static final boolean UNBOUNDID_LOG_ENABLE_DEFAULT = false;

    /**
     * The name of the configuration property in the standard idm property file. Valid values are the full pathname
     * of the class as an instance will be instantiated. The class must have a default constructor and implement java.util.logging.Formatter.
     * For example, "java.util.logging.XMLFormatter" or "java.util.logging.SimpleFormatter". SimpleFormatter is the default
     * value when not provided.
     */
    public static final String UNBOUNDID_LOG_FORMATTER_CLASS_PROP_NAME = "unboundid.log.formatter.class";

    /**
     * The default formatter to use when a formatter is not specified in the configuration file.
     */
    public static final String UNBOUNDID_LOG_FORMATTER_CLASS_DEFAULT = "java.util.logging.SimpleFormatter";

    private java.util.logging.Logger unboundLogger = Debug.getLogger();

    @Autowired
    AnalyticsLogger analyticsLogger;

    @Autowired
    private Configuration config;

    private boolean unboundIdLoggingEnabled = false;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        ServletContext servletContext = filterConfig.getServletContext();
        WebApplicationContext webApplicationContext = WebApplicationContextUtils.getWebApplicationContext(servletContext);

        AutowireCapableBeanFactory autowireCapableBeanFactory = webApplicationContext.getAutowireCapableBeanFactory();

        autowireCapableBeanFactory.configureBean(this, "analyticsLogger");

        configureUnboundLogging();
    }

    /**
     * When enabled unboundId logging will log all requests/responses made to CA and write to a log file. Logging
     * is EXTREMELY intensive and should never be done in a prod environment.
     */
    private void configureUnboundLogging() {
        boolean enableUnboundIdLogging = config.getBoolean(UNBOUNDID_LOG_ENABLE_PROP_NAME, UNBOUNDID_LOG_ENABLE_DEFAULT);
        if (enableUnboundIdLogging) {
            String logFilePath = config.getString(UNBOUNDID_LOG_LOCATION_PROP_NAME, UNBOUNDID_LOG_LOCATION_DEFAULT);
            String formatterClassName = config.getString(UNBOUNDID_LOG_FORMATTER_CLASS_PROP_NAME, UNBOUNDID_LOG_FORMATTER_CLASS_DEFAULT);
            try {
                Class formatterClass = Class.forName(formatterClassName);
                Formatter logFormatter = (Formatter) formatterClass.newInstance();

                Debug.setEnabled(true);
                Debug.setIncludeStackTrace(true);
                Logger logger = Debug.getLogger();
                logger.setLevel(Level.FINEST);
                for (final Handler handler : logger.getHandlers()) {
                    handler.setLevel(Level.FINEST);
                }

                FileHandler fileHandler = new FileHandler(logFilePath);
                fileHandler.setLevel(Level.FINEST);
                fileHandler.setFormatter(logFormatter);
                logger.addHandler(fileHandler);
                unboundIdLoggingEnabled = true;
            } catch (ClassNotFoundException e) {
                LOG.error(String.format("unboundid logging format class '%s' was not found. Disabling logging.", formatterClassName), e);
            } catch (InstantiationException e) {
                LOG.error(String.format("Instantiating unboundid logging format class '%s' encountered an error. Disabling logging.", formatterClassName), e);
            } catch (IllegalAccessException e) {
                LOG.error(String.format("Instantiating unboundid logging format class '%s' encountered an error. Disabling logging.", formatterClassName), e);
            } catch (ClassCastException e) {
                LOG.error(String.format("unboundid logging format class '%s' must be of type java.util.logging.Formatter. Disabling logging.", formatterClassName), e);
            } catch (IOException e) {
                throw new RuntimeException("Error configuring unboundid logger. Disabling logging.", e);
            } catch (Exception e) {
                LOG.error("Exception encountered initializing unboundid logging. Disabling logging.", e);
            }
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        MultiReadHttpServletRequest requestWrapper = new MultiReadHttpServletRequest((HttpServletRequest) request);

        String path = ((HttpServletRequestWrapper) requestWrapper).getPathInfo();
        String method = ((HttpServletRequestWrapper) requestWrapper).getMethod();

        try {
            if (unboundIdLoggingEnabled) {
                unboundLogger.fine(String.format("** START %s %s **", method, path));
            }

            String host = ((HttpServletRequestWrapper) requestWrapper).getHeader("Host");
            String remoteHost = ((HttpServletRequestWrapper) requestWrapper).getRemoteHost();
            String userAgent = ((HttpServletRequestWrapper) requestWrapper).getHeader("User-Agent");
            String authToken = ((HttpServletRequestWrapper) requestWrapper).getHeader("X-Auth-Token");
            String basicAuth = ((HttpServletRequestWrapper) requestWrapper).getHeader("Authorization");
            InputStream stream = requestWrapper.getInputStream();
            String requestBody = IOUtils.toString(stream);
            String requestType = ((HttpServletRequestWrapper) requestWrapper).getHeader("Content-Type");
            Long startTime = new Date().getTime();

            StatusExposingServletResponse responseWrapper = new StatusExposingServletResponse((HttpServletResponse) response);

            chain.doFilter(requestWrapper, responseWrapper);
            int status = responseWrapper.getStatus();
            String responseBody = responseWrapper.getBody();
            String responseType = ((HttpServletRequestWrapper) requestWrapper).getHeader("Accept");

            if(config.getBoolean("analytics.logger.enabled", false)) {
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
        } finally {
            if (unboundIdLoggingEnabled) {
                unboundLogger.fine(String.format("** END %s %s **", method, path));
            }
        }
    }

    @Override
    public void destroy() {
    }
}
