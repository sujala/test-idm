<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Rackspace Login</title>
</head>
<body>
<form name='input' action='authorize' method='POST'>Username: <input
	type='text' name='username' /><br />
Password: <input type='password' name='password' /><span style="color:red"><%= request.getAttribute("error") == null ? "" : request.getAttribute("error") %></span><br />
<input type='submit' value='Login' /> <input type='hidden'
	name='redirect_uri' value='<%= request.getParameter("redirect_uri") %>'> <input type='hidden'
	name='client_id' value='<%= request.getParameter("client_id") %>'> <input type='hidden'
	name='response_type' value='<%= request.getParameter("response_type") %>'> <input type='hidden'
	name='scope' value='<%= request.getParameter("scope") %>'></form>
</body>
</html>