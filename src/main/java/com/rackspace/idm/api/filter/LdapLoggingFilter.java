package com.rackspace.idm.api.filter;

import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.exception.ForbiddenException;
import com.rackspace.idm.exception.NotAuthorizedException;
import com.unboundid.util.Debug;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.*;

/**
 * Allows end users to log all ldap requests made in response to handling an IDM service request. Allows the user to either
 * receive the location of the log file for subsequent retrieval, or to replace the content of the response with the log
 * instead of the normal response body.
 * <p/>
 * The code for replacing the body with something else (CharResponseWrapper) was based on http://stackoverflow.com/questions/14736328/looking-for-an-example-for-inserting-content-into-the-response-using-a-servlet-f
 */
@Component
public class LdapLoggingFilter extends OncePerRequestFilter {
    private static org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(LdapLoggingFilter.class);
    private java.util.logging.Logger unboundLogger = Debug.getLogger();

    public static final String UNBOUND_LOG_ALLOW_PROP_NAME = "allow.ldap.logging";

    public static final String UNBOUNDID_LOG_LOCATION_PROP_NAME = "unboundid.log.location";
    public static final String UNBOUNDID_LOG_LOCATION_DEFAULT = System.getProperty("java.io.tmpdir");

    public static final boolean UNBOUND_LOG_ALLOW_DEFAULT = false;

    public static final String HEADER_CREATE_LDAP_LOG = "X-CREATE-LDAP-LOG";
    public static final String HEADER_X_LDAP_LOG_TOKEN = "X-LDAP-LOG-TOKEN";
    public static final String HEADER_X_RETURN_LDAP_LOG = "X-RETURN-LDAP-LOG";
    public static final String HEADER_X_LDAP_LOG_LOCATION = "X-LDAP-LOG-LOCATION";
    public static final String HEADER_X_LDAP_LOG_SERVICE_STATUS_CODE = "X-LDAP-LOG-SERVICE-STATUS-CODE";

    public static final String UNBOUNDID_LOG_FORMATTER_CLASS_DEFAULT = "java.util.logging.XMLFormatter";

    public static final String UNBOUNDID_LOG_EXTENSION = ".log";

    @Autowired
    private Configuration globalConfig;

    @Autowired
    private ScopeAccessService scopeAccessService;

    @Autowired
    private AuthorizationService authorizationService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!isLdapLoggingAllowed()) {
            //do nothing.
            filterChain.doFilter(request, response);
        } else {
            LdapLoggingMeta meta = null;
            meta = parseLdapLoggingMetaFromRequest(request);
            if (meta.logLdap) {
                try {
                    runLoggingForRequest(meta, request, response, filterChain);
                } catch (NotAuthorizedException ex) {
                    String errMsg = "{\"unauthorized\":{\"code\":401,\"message\":\"" + ex.getMessage() + "\"}}";
                    response.setStatus(HttpStatus.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write(errMsg);
                } catch (ForbiddenException ex) {
                    String errMsg = "{\"forbidden\":{\"code\":403,\"message\":\"" + ex.getMessage() + "\"}}";
                    response.setStatus(HttpStatus.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write(errMsg);
                } catch (Exception e) {
                    LOG.error("Tried to enable ldap logging, but failed", e);
                    String errMsg = "{\"Server Error\":{\"code\":500,\"message\":\"Error\"}}"; //don't display detailed info
                    response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write(errMsg);
                }
            } else {
                //do nothing.
                filterChain.doFilter(request, response);
            }
        }
    }

    private LdapLoggingMeta parseLdapLoggingMetaFromRequest(HttpServletRequest request) {
        try {
            String logRequestStr = request.getHeader(HEADER_CREATE_LDAP_LOG);
            String replaceResponseStr = request.getHeader(HEADER_X_RETURN_LDAP_LOG);
            String authTokenStr = request.getHeader(HEADER_X_LDAP_LOG_TOKEN);

            boolean logRequest = Boolean.valueOf(logRequestStr);
            boolean replaceResponseWithLog = Boolean.valueOf(replaceResponseStr);

            return new LdapLoggingMeta(logRequest, replaceResponseWithLog, authTokenStr);
        } catch (Exception e) {
            LOG.error("Error parsing ldap logger headers. Logging will not occur for this request.", e);
            return new LdapLoggingMeta(false, false, null);
        }
    }

    private void runLoggingForRequest(LdapLoggingMeta meta, HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        ScopeAccess scopeAccess = getScopeAccessForValidServiceAdminToken(meta.token); //auth user

        CharResponseWrapper wrappedResponse = new CharResponseWrapper(response);

        File logDir = getLogParentDir();
        File logFile = File.createTempFile("ldapLog_", UNBOUNDID_LOG_EXTENSION, logDir);
        enableUnboundLogging(logFile);
        unboundLogger.fine(String.format("** START %s %s **", request.getMethod(), request.getPathInfo()));

        filterChain.doFilter(request, wrappedResponse);

        byte[] bytes = wrappedResponse.getByteArray();
        unboundLogger.fine(String.format("** END %s %s **", request.getMethod(), request.getPathInfo()));
        disableUnboundLogging();

        //set the header
        response.addHeader(HEADER_X_LDAP_LOG_LOCATION, getLogURL(request, logFile.getName()));

        if (meta.replaceResponseWithLog) {
            response.addHeader(HEADER_X_LDAP_LOG_SERVICE_STATUS_CODE, String.valueOf(wrappedResponse.httpStatus));
            response.setContentType("application/xml");
            response.setStatus(HttpServletResponse.SC_OK);
            bytes = readLog(logFile.getName());
        }
        response.getOutputStream().write(bytes);

    }

    private byte[] readLog(String logName) {
        try {
            File logDir = getLogParentDir();
            File logFile = new File(logDir, logName);
            String logContents = FileUtils.readFileToString(logFile);
            return logContents.getBytes();
        } catch (IOException e) {
            LOG.error("Encountered exception creating ldap log. Logging disabled", e);
            throw new RuntimeException("Error retrieving log", e);
        }
    }

    public static String getLogURL(HttpServletRequest req, String logName) {
        String scheme = req.getScheme();             // http
        String serverName = req.getServerName();     // hostname.com
        int serverPort = req.getServerPort();        // 80
        String contextPath = req.getContextPath();   // /mywebapp
        String servletPath = req.getServletPath();   // /servlet/MyServlet

        String logPath = "/devops/ldap/log/";

        // Construct url
        StringBuilder url = new StringBuilder();
        url.append(scheme).append("://").append(serverName);

        //only add port if not default for that scheme
        if (!(
                ("http".equals(scheme) && serverPort == 80)
                        || ("https".equals(scheme) && serverPort != 443))
                ) {
            url.append(":").append(serverPort);
        }

        url.append(contextPath).append(servletPath).append(logPath).append(logName);

        return url.toString();
    }

    private File getLogParentDir() {
        String logFileDir = globalConfig.getString(UNBOUNDID_LOG_LOCATION_PROP_NAME, UNBOUNDID_LOG_LOCATION_DEFAULT);
        File logDir = new File(logFileDir);
        if (!logDir.exists()) {
            if (!logDir.mkdirs()) {
                throw new IllegalStateException("Could not create directories '" + logFileDir + " for ldap log files.");
            }
        }
        if (!logDir.isDirectory()) {
            throw new IllegalArgumentException("'" + logFileDir + "' is not a directory. Please set " + UNBOUNDID_LOG_LOCATION_PROP_NAME + " to a directory.");
        }
        return logDir;
    }

    private ScopeAccess getScopeAccessForValidServiceAdminToken(String authToken) {
        String errMsg = String.format("Must provide %s header with an appropriate valid token to perform this action", LdapLoggingFilter.HEADER_X_LDAP_LOG_TOKEN);
        if (StringUtils.isBlank(authToken)) {
            throw new NotAuthorizedException(errMsg);
        }
        ScopeAccess authScopeAccess = this.scopeAccessService.getScopeAccessByAccessToken(authToken);

        if (authScopeAccess == null || authScopeAccess.isAccessTokenExpired(new DateTime())) {
            throw new NotAuthorizedException(errMsg);
        }
        if (!authorizationService.authorizeCloudServiceAdmin(authScopeAccess)) {
            throw new ForbiddenException(errMsg);
        }
        return authScopeAccess;
    }

    private boolean isLdapLoggingAllowed() {
        return globalConfig.getBoolean(UNBOUND_LOG_ALLOW_PROP_NAME, UNBOUND_LOG_ALLOW_DEFAULT);
    }

    private class LdapLoggingMeta {
        private boolean logLdap = false;
        private boolean replaceResponseWithLog = false;
        private String token;

        private LdapLoggingMeta(boolean logLdap, boolean replaceResponseWithLog, String token) {
            this.logLdap = logLdap;
            this.token = token;
            this.replaceResponseWithLog = replaceResponseWithLog;
        }
    }

    /**
     * When enabled unboundId logging will log all requests/responses made to CA and write to a log file.
     */
    private void enableUnboundLogging(File logFilePath) {
        String formatterClassName = UNBOUNDID_LOG_FORMATTER_CLASS_DEFAULT;
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

            FileHandler fileHandler = new FileHandler(logFilePath.getAbsolutePath());
            fileHandler.setLevel(Level.FINEST);
            fileHandler.setFormatter(logFormatter);
            logger.addHandler(fileHandler);
        } catch (Exception e) {
            LOG.error("Exception encountered initializing unboundid logging.", e);
            throw new RuntimeException(e);
        }
    }

    private void disableUnboundLogging() {
        Debug.setEnabled(false);
    }

    private class ByteArrayServletStream extends ServletOutputStream {
        ByteArrayOutputStream baos;

        ByteArrayServletStream(ByteArrayOutputStream baos) {
            this.baos = baos;
        }

        public void write(int param) throws IOException {
            baos.write(param);
        }
    }

    public class ByteArrayPrintWriter {
        private ByteArrayOutputStream baos = new ByteArrayOutputStream();
        private PrintWriter pw = new PrintWriter(baos);
        private ServletOutputStream sos = new ByteArrayServletStream(baos);

        public PrintWriter getWriter() {
            return pw;
        }

        public ServletOutputStream getStream() {
            return sos;
        }

        byte[] toByteArray() {
            return baos.toByteArray();
        }
    }

    public class CharResponseWrapper extends HttpServletResponseWrapper {
        private ByteArrayPrintWriter output;
        private boolean usingWriter;
        int httpStatus;

        public CharResponseWrapper(HttpServletResponse response) {
            super(response);
            usingWriter = false;
            output = new ByteArrayPrintWriter();
        }

        public byte[] getByteArray() {
            return output.toByteArray();
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            // will error out, if in use
            if (usingWriter) {
                super.getOutputStream();
            }
            usingWriter = true;
            return output.getStream();
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            // will error out, if in use
            if (usingWriter) {
                super.getWriter();
            }
            usingWriter = true;
            return output.getWriter();
        }

        @Override
        public void setStatus(int sc) {
            httpStatus = sc;
            super.setStatus(sc);
        }

        public String toString() {
            return output.toString();
        }
    }
}