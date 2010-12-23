<%@ include file="globalfuncs.jsp" %>
<%@ page import="services.IdmService" %>
<%
response.setHeader("Cache-Control","no-cache");
response.setHeader("Pragma","no-cache");
response.setDateHeader ("Expires", -1);

String tokenType = getParam(request, "tokenType");
String clientId = getParam(request, "clientId");
String clientSecret = getParam(request, "clientSecret");
String username = getParam(request, "username");
String password = getParam(request, "password");
String refreshToken = getParam(request, "refreshToken");

IdmService idmService = new IdmService();
String result = idmService.getToken(tokenType, 
        clientId, clientSecret, username, password, refreshToken);

out.print(result);
%>
