package services;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class IdmService {

    private static String HTTP_CONTENT_TYPE = "application/xml";
    private static String HTTP_ACCEPT_TYPE = "application/xml";

    protected String apiUrl = "http://localhost:8080/v1.0";
    protected String apiNamespace = "http://docs.rackspacecloud.com/idm/api/v1.0";
    protected String accessToken = "";

    protected Map httpHeaders = new HashMap();

    public IdmService() {
        httpHeaders.put("Accept", HTTP_ACCEPT_TYPE);
    }
    public IdmService(String accessToken)
    {
        this();
        
        this.accessToken = accessToken;
        if (!accessToken.equals("")) {
            httpHeaders.put("Authorization", String.format("OAuth %s", this.accessToken));
        }
    }

    /**
     * Get an access token
     *
     * @param string clientId
     * @param string clientSecret
     * @param string username
     * @param string password
     *
     * @return string
     */
    public String getToken(String tokenType, String clientId,
        String clientSecret, String username,
        String password, String refreshToken){

        String grantType = "NONE";
        if (tokenType.equals("admin") ||
            tokenType.equals("non-admin") ||
            tokenType.equals("racker")) {

            grantType = "PASSWORD";
        }
        else if (tokenType.equals("refresh-token")) {
            grantType = "REFRESh_TOKEN";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<authCredentials ");
        sb.append(String.format("xmlns=\"%s\" ", this.apiNamespace));
        sb.append(String.format("client_id=\"%s\" ", clientId));
        sb.append(String.format("client_secret=\"%s\" ", clientSecret));
        sb.append(String.format("username=\"%s\" ", username));
        sb.append(String.format("password=\"%s\" ", password));
        sb.append(String.format("refresh_token=\"%s\" ", refreshToken));
        sb.append(String.format("grant_type=\"%s\" ", grantType));
        sb.append(" />");
        
        String data = sb.toString();

        String url = this.apiUrl + "/token";

        String result = sendPost(url, data);

        return result;
    }

    /**
     * Add a user
     *
     * @param string customerNumber
     * @param string username
     * @param string password
     * @param string firstName
     * @param string lastName
     * @param string middleName
     * @param string email
     * @param string secretQuestion
     * @param string secretAnswer
     *
     * @return string
     */
    public String addUser(String customerNumber, String username, String password,
        String firstName, String lastName, String middleName, String email,
        String secretQuestion, String secretAnswer) {

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("<user xmlns=\"%s\" ", this.apiNamespace));
        sb.append("softDeleted=\"false\" locked=\"false\" status=\"ACTIVE\" ");
        sb.append("region=\"America/Chicago\" ");
        sb.append("prefLanguage=\"US_en\" ");
        sb.append(String.format("displayName=\"%s %s\" ", firstName, lastName));
        sb.append(String.format("lastName=\"%s\" ", lastName));
        sb.append(String.format("firstName=\"%s\" ", firstName));
        sb.append(String.format("email=\"%s\" ", email));
        sb.append(String.format("customerId=\"%s\" ", customerNumber));
        sb.append(String.format("username=\"%s\" ", username));
        sb.append("> ");

        sb.append("<secret ");
        sb.append(String.format("secretQuestion=\"%s\" ", secretQuestion));
        sb.append(String.format("secretAnswer=\"%s\" ", secretAnswer));
        sb.append("/>");

        sb.append("<password ");
        sb.append(String.format("password=\"%s\" ", password));
        sb.append("/>");
        sb.append("</user>");

        String data = sb.toString();

        String url = this.apiUrl + "/customers/" + customerNumber + "/users";
        String result = sendPost(url, data);

        return result;
    }

    /**
     * Update a user
     *
     * @param string customerNumber
     * @param string username
     * @param string firstName
     * @param string lastName
     * @param string middleName
     * @param string email
     *
     * @return string
     */
    public String updateUser(String customerNumber, String username,
        String firstName, String lastName, String middleName, String email,
        String secretQuestion, String secretAnswer) {

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("<user xmlns=\"%s\" ", this.apiNamespace));
        sb.append("softDeleted=\"false\" locked=\"false\" status=\"ACTIVE\" ");
        sb.append("region=\"America/Chicago\" ");
        sb.append("prefLanguage=\"US_en\" ");
        sb.append(String.format("displayName=\"%s %s\" ", firstName, lastName));
        sb.append(String.format("lastName=\"%s\" ", lastName));
        sb.append(String.format("firstName=\"%s\" ", firstName));
        sb.append(String.format("email=\"%s\" ", email));
        sb.append(String.format("customerId=\"%s\" ", customerNumber));
        sb.append(String.format("username=\"%s\" ", username));
        sb.append("> ");

        sb.append("<secret ");
        sb.append(String.format("secretQuestion=\"%s\" ", secretQuestion));
        sb.append(String.format("secretAnswer=\"%s\" ", secretAnswer));
        sb.append("/>");

        sb.append("</user>");

        String data = sb.toString();

        String url = this.apiUrl + "/customers/" + customerNumber + "/users/" + username;
        String result = sendPut(url, data);

        return result;
    }

    /**
     * Get a user
     *
     * @param string customerNumber
     * @param string username
     *
     * @return string
     */
    public String getUser(String customerNumber, String username) {

        String url = this.apiUrl + "/customers/" + customerNumber + "/users/" + username;
        String result = sendGet(url);

        return result;
    }

    /**
     * Delete a user
     *
     * @param string customerNumber
     * @param string username
     *
     * @return string
     */
    public String deleteUser(String customerNumber, String username) {

        String url = this.apiUrl + "/customers/" + customerNumber + "/users/" + username;
        String result = this.sendDelete(url);

        return result;
    }

    /* -------------------- Private Funcs -----------------------*/

    /**
     * Send Post request
     *
     * @param string url
     * @param string data
     *
     * @return string
     */
    private String sendPost(String url, String data) {

        HttpClientService httpClient = new HttpClientService();
        String responseBody = "";
        int statusCode = 0;

        try {
            HttpResponse response = httpClient.sendPost(url, HTTP_CONTENT_TYPE, this.httpHeaders, data);
            responseBody = httpClient.getResponseBody(response);
            statusCode = response.getStatusLine().getStatusCode();
        }
        catch (Exception e) {
            responseBody = "<error>" + e.getMessage() + "</error>";
        }

        String retval = buildReturnXml(responseBody, statusCode);
        
        return retval;
    }

    /**
     * Send Put request
     *
     * @param string url
     * @param string data
     *
     * @return string
     */
    private String sendPut(String url, String data) {

        HttpClientService httpClient = new HttpClientService();
        String responseBody = "";
        int statusCode = 0;

        try {
            HttpResponse response = httpClient.sendPut(url, HTTP_CONTENT_TYPE, this.httpHeaders, data);
            responseBody = httpClient.getResponseBody(response);
            statusCode = response.getStatusLine().getStatusCode();
        }
        catch (Exception e) {
            responseBody = "<error>" + e.getMessage() + "</error>";
        }

        String retval = buildReturnXml(responseBody, statusCode);

        return retval;
    }

    /**
     * Send Get request
     *
     * @param string url
     * @param array() options
     *
     * @return string
     */
    private String sendGet(String url) {

        HttpClientService httpClient = new HttpClientService();
        String responseBody = "";
        int statusCode = 0;

        try {
            HttpResponse response = httpClient.sendGet(url, this.httpHeaders);
            responseBody = httpClient.getResponseBody(response);
            statusCode = response.getStatusLine().getStatusCode();
        }
        catch (Exception e) {
            responseBody = "<error>" + e.getMessage() + "</error>";
        }
        
        String retval = buildReturnXml(responseBody, statusCode);

        return retval;
    }

    /**
     * Send Delete request
     *
     * @param string url
     * @return string
     */
    private String sendDelete(String url) {

        HttpClientService httpClient = new HttpClientService();
        String responseBody = "";
        int statusCode = 0;

        try {
            HttpResponse response = httpClient.sendDelete(url, this.httpHeaders);
            responseBody = httpClient.getResponseBody(response);
            statusCode = response.getStatusLine().getStatusCode();
        }
        catch (Exception e) {
            responseBody = "<error>" + e.getMessage() + "</error>";
        }

        String retval = buildReturnXml(responseBody, statusCode);

        return retval;
    }

    private String buildReturnXml(String responseBody, int statusCode) {

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        sb.append("<root>");
        try {
            sb.append(String.format("<result>%s</result>", URLEncoder.encode(responseBody,"UTF-8")));
        } catch (UnsupportedEncodingException e) {
            sb.append("<result></result>");
        }
        sb.append(String.format("<statusCode>%s</statusCode>", statusCode));
        sb.append("</root>");

        String retval = sb.toString();

        return retval;
    }
}