package com.rackspace.idm.api.filter;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * <p>
 * The filter adds response headers to every response that it is
 * mapped to. The initParameter <strong>Headers</strong> denotes the
 * headers to add to the response. Set this value to the actual
 * headers.  For example:</p>
 * <code>
 * Vary: Accept, Accept-Encoding
 * </code>
 * <p>
 * You can supply multiple headers one per line. If the same header is
 * specified twice it will be combined into a comma seperated list.
 * </p>
 * <code>
 * Vary: Accept
 * Vary: Accept-Encoding
 * </code>
 * <p>
 * The filter does not replace existing http headers.
 * </p>
 *
 * @author jorgew
 */
public class ResponseFilter implements Filter {
    private static final String HEADER_PARAM = "Headers";
    private Map<String, String> headers;

    public void init(FilterConfig filterConfig) throws ServletException {
        String allHeaders = filterConfig.getInitParameter(HEADER_PARAM);

        if (allHeaders == null) {
            throw new ServletException("The ResponseHeaderFilter requires a " + HEADER_PARAM + " init parameter");
        }
        headers = new HashMap<String, String>();

        //
        // Seperate by lines then strip whitespace
        //
        StringTokenizer lineTokenizer = new StringTokenizer(allHeaders, "\n\r");
        while (lineTokenizer.hasMoreTokens()) {
            String line = lineTokenizer.nextToken().trim();
            if (line.length() != 0) {
                //
                // Seperate name and value via by ':'
                //
                StringTokenizer headerTokenizer = new StringTokenizer(line, ":");
                if (headerTokenizer.countTokens() != 2) {
                    throw new ServletException("ResponseHeaderFilter: Expecting header in format  'HeaderName: Value'");
                }

                //
                //  Retrive header name and value.  We convert
                //  headerName to lower case since headers are
                //  case insensitive and we want to correctly
                //  catch multiple same-name headers.
                //
                String headerName = headerTokenizer.nextToken().toLowerCase();
                String headerValue = headerTokenizer.nextToken();

                String oldValue = headers.get(headerName);
                if (oldValue != null) {
                    //
                    //  As defined in the HTTP Spec (4.2)
                    //  Multiple Headers of the same name
                    //  may be specefied as a single comma
                    //  seperated list.
                    //
                    headerValue = oldValue + ", " + headerValue;
                }
                headers.put(headerName, headerValue);
            }
        }
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        Set<Entry<String, String>> entries = headers.entrySet();
        for (Entry<String, String> e : entries) {
            if (!((HttpServletResponse) response).containsHeader("response-source")) {
                ((HttpServletResponse) response).setHeader(e.getKey(), e.getValue());
            }
        }
        chain.doFilter(request, response);
    }

    public void destroy() {
    }
}
