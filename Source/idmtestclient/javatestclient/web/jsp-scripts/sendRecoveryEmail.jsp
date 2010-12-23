<%@ include file="globalfuncs.jsp" %>
<%@ page import="services.IdmService" %>
<%
response.setHeader("Cache-Control","no-cache");
response.setHeader("Pragma","no-cache");
response.setDateHeader ("Expires", -1);

String accessToken = getParam(request, "accessToken");
String customerNumber = getParam(request, "customerNumber");
String username = getParam(request, "username");

String subject = getParam(request, "subject");
String replyTo = getParam(request, "replyTo");
String from = getParam(request, "from");

String templateUrl = getParam(request, "templateUrl");
String callbackUrl = getParam(request, "callbackUrl");
String firstName = getParam(request, "firstName");

IdmService idmService = new IdmService(accessToken);
String result = idmService.sendRecoveryEmail(customerNumber, username,
        subject, replyTo, from, templateUrl, callbackUrl, firstName);

out.print(result);
%>
