<%@ include file="globalfuncs.jsp" %>
<%@ page import="services.IdmService" %>
<%
response.setHeader("Cache-Control","no-cache");
response.setHeader("Pragma","no-cache");
response.setDateHeader ("Expires", -1);

String accessToken = getParam(request, "accessToken");
String customerNumber = getParam(request, "customerNumber");
String clientId = getParam(request, "clientId");

IdmService idmService = new IdmService(accessToken);
String result = idmService.getClient(customerNumber, clientId);

out.print(result);
%>
