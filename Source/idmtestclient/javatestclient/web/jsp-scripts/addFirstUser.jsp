<%@ include file="globalfuncs.jsp" %>
<%@ page import="services.IdmService" %>
<%
response.setHeader("Cache-Control","no-cache");
response.setHeader("Pragma","no-cache");
response.setDateHeader ("Expires", -1);

String accessToken = getParam(request, "accessToken");

String customerNumber = getParam(request, "customerNumber");
String username = getParam(request, "username");
String password = getParam(request, "password");
String firstName = getParam(request, "firstName");
String lastName = getParam(request, "lastName");
String middleName = getParam(request, "middleName");
String email = getParam(request, "email");
String secretQuestion = getParam(request, "secretQuestion");
String secretAnswer = getParam(request, "secretAnswer");

IdmService idmService = new IdmService(accessToken);
String result = idmService.addFirstUser(customerNumber, username, password,
        firstName, lastName, middleName, email, secretQuestion, secretAnswer);

out.print(result);
%>


