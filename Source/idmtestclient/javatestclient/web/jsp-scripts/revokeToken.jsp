<%@ include file="globalfuncs.jsp" %>
<%@ page import="services.IdmService" %>
<%
response.setHeader("Cache-Control","no-cache");
response.setHeader("Pragma","no-cache");
response.setDateHeader ("Expires", -1);

String accessToken = getParam(request, "accessToken");
String tokenToRevoke = getParam(request, "tokenToRevoke");

IdmService idmService = new IdmService(accessToken);
String result = idmService.revokeToken(tokenToRevoke);

out.print(result);
%>