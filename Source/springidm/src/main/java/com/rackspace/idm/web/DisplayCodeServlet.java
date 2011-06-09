package com.rackspace.idm.web;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

public class DisplayCodeServlet extends HttpServlet  {
    
    // FIXME: This class should be removed its only here for to help QA test
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    }
    
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    }
    
    public void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PrintWriter out = response.getWriter();

        String error = request.getParameter("error");
        String code = request.getParameter("code");
        
        if (StringUtils.isBlank(code)) {
            out.println("error=" + error);
        }
        else {
            out.println("code=" + code);
        }
    }
}
