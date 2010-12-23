<%@ include file="globalfuncs.jsp" %>
<%@ page import="services.IdmService" %>
<%
response.setHeader("Cache-Control","no-cache");
response.setHeader("Pragma","no-cache");
response.setDateHeader ("Expires", -1);

String accessToken = getParam(request, "accessToken");
String tokenToValidate = getParam(request, "tokenToValidate");

IdmService idmService = new IdmService(accessToken);
String result = idmService.validateToken(tokenToValidate);

out.print(result);
%>