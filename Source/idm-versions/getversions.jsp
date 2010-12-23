<%@ page language="java" contentType="application/json; charset=utf-8" pageEncoding="utf-8"%><%
String xmlOut = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
    "<versions xmlns=\"http://docs.rackspacecloud.com/idm/v1.0\">" +
    "<version id=\"v1.0\" status=\"BETA\" href=\"/v1.0\"/>" +
    "</versions>";

String jsonOut = "{" +
    "\"versions\" : [" +
        "{" +
            "\"id\": \"v1.0\", " +
            "\"status\": \"BETA\", " +
            "\"href\"  : \"/v1.0\" " +
        "}" +
    "]}";

String acceptHeader = request.getHeader("Accept");
acceptHeader = acceptHeader.toLowerCase();
if (acceptHeader.equals("application/xml")) { 
    response.setContentType("application/xml");
    out.print(xmlOut);
} else {
    out.print(jsonOut);
}
%>