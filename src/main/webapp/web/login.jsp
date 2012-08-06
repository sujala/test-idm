<%@page import="org.apache.commons.lang.StringEscapeUtils"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<link rel="stylesheet" href="../styles/idm.css" type="text/css">
<title>Login</title>
</head>
<body>
<table id="login-wrapper">
	<tbody>
		<tr>

			<td id="login-wrapper-cell">

			<div id="dialogContent">
			<div id="dialogTop">
			<form id="LoginForm" name='LoginForm' action='authorize'
				method='POST' class="form">&nbsp;
			<div class="field"><label for="username"><strong>Username</strong></label><img
				class="fieldDot" width="10" height="10"
				src="../images/field_optional.gif" alt=""
				name="username"></img><input tabindex="1" type="text" size="40"
				name="username" id="username" value="<%=request.getParameter("username") == null ? "" : StringEscapeUtils.escapeHtml(request.getParameter("username")) %>"></input><p></p>
				</div>
			<div class="field"><label for="password"><strong>Password</strong></label><img
				class="fieldDot" width="10" height="10"
				src="../images/field_optional.gif" alt=""
				name="password"></img><input tabindex="2" type="password" size="40"
				name="password" id="password"></input><p><img
                class="fieldDot" width="10" height="10"
                src="../images/field_optional.gif" alt=""><span style="color: red"><%=request.getAttribute("error") == null ? "" : request
                .getAttribute("error")%></span></p>
				</div>
			<p id="buttonRow">
			<button id="submitButton" type="submit" tabindex="3"><span>Login</span></button>
			</p>
			<input type='hidden' name='redirect_uri'
				value='<%=StringEscapeUtils.escapeHtml(request.getParameter("redirect_uri"))%>'> <input
				type='hidden' name='client_id'
				value='<%=StringEscapeUtils.escapeHtml(request.getParameter("client_id"))%>'> <input
				type='hidden' name='response_type'
				value='<%=StringEscapeUtils.escapeHtml(request.getParameter("response_type"))%>'> <input
				type='hidden' name='scope'
				value='<%=StringEscapeUtils.escapeHtml(request.getParameter("scope"))%>'></form>
			</div>
			<div id="dialogBottom"></div>
			</div>
			</td>
		</tr>
	</tbody>
</table>
</body>
</html>
