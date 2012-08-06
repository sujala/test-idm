package com.rackspace.idm.web;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * This class is responsible for displaying static content that might exist in the web application.
 * This allows us view static content even if a framework servlet e.g Jersey Servlet, is mapped to
 * a directory structure that contains the static content e.g /*
 *
 */
public class DelegatePassThroughServlet extends HttpServlet  {
    
    private static final long serialVersionUID = 3333760951987802617L;

    @Override
    public void doGet(final HttpServletRequest request, final HttpServletResponse response) 
    	throws ServletException, IOException{
        // gets the default request dispatcher from underlying web container
        final RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("default");

        /*
         *  Need this wrapper
         *  Request dispatcher is changing wildcard url mapping.
         *  e.g. /docs/index.html  is changed to
         *  docs  as servletpath, index.html as path.
         *  This wrapper reverts these changes
         */
        final HttpServletRequest wrapped = new HttpServletRequestWrapper(request) {
            @Override
            public String getServletPath() {
                return "";
            }

            @Override
            public String getPathInfo() {
                return request.getServletPath() + request.getPathInfo();
            }
        };
            
        dispatcher.forward(wrapped, response);
    }
}
