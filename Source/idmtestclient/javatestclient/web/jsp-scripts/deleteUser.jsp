<%@ include file="globalfuncs.jsp" %>
<%@ page import="services.IdmService" %>
<%
response.setHeader("Cache-Control","no-cache");
response.setHeader("Pragma","no-cache");
response.setDateHeader ("Expires", -1);

String accessToken = getParam(request, "accessToken");

String customerNumber = getParam(request, "customerNumber");
String username = getParam(request, "username");

IdmService idmService = new IdmService(accessToken);
String result = idmService.deleteUser(customerNumber, username);

out.print(result);
%>

