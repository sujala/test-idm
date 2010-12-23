<%@ page import="java.net.URLDecoder"%>
<%@ page import="java.io.UnsupportedEncodingException"%>
<%!

public String getParam(HttpServletRequest request, String paramName)
{
    String paramVal = request.getParameter(paramName) != null ?
        request.getParameter(paramName) : "";

    try {
    paramVal = URLDecoder.decode(paramVal, "UTF-8");
    }
    catch (UnsupportedEncodingException e) {
        paramVal = "";
    }
    return paramVal;
}
%>