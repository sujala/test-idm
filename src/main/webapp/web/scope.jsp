<%@page import="com.rackspace.idm.domain.entity.Application"%>
<%@page import="org.apache.commons.lang.StringEscapeUtils"%>
<%@page import="java.util.List"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Scope Acceptance</title>
</head>
<body>
<form name='input' action='accept' method='POST'>
<p><%= ((Application) request.getAttribute("requestingClient")).getTitle() %></p>
<p><%= ((Application) request.getAttribute("requestingClient")).getDescription() %></p>
<p>wants access to</p>
<ul><% for (Application c : (List<Application>) request.getAttribute("scopes")) {%>
<li><%= c.getTitle() %> - <%= c.getDescription() %></li>
<% } %></ul>
<br />
<select name="days">
<option value="1">One Day</option>
<option value="7">One Week</option>
<option value="30">One Month</option>
<option value="365">One Year</option>
<option value="-1" selected="selected">Until Canceled</option>
</select>
<input name='accept' type='submit' value='Accept' />
<input name='accept' type='submit' value='Deny' />
<input type='hidden' name='redirect_uri' value='<%= StringEscapeUtils.escapeHtml(request.getParameter("redirect_uri")) %>'> 
<input type='hidden' name='client_id' value='<%= StringEscapeUtils.escapeHtml(request.getParameter("client_id")) %>'> 
<input type='hidden' name='response_type' value='<%= StringEscapeUtils.escapeHtml(request.getParameter("response_type")) %>'> 
<input type='hidden' name='scope' value='<%= StringEscapeUtils.escapeHtml(request.getParameter("scope")) %>'> 
<input type='hidden' name='username' value='<%= StringEscapeUtils.escapeHtml(request.getAttribute("username").toString()) %>'>
<input type='hidden' name='verification' value='<%= StringEscapeUtils.escapeHtml(request.getAttribute("verification").toString()) %>'>
</form>
</body>
</html>