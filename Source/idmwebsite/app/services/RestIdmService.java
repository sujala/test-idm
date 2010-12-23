package services;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import configuration.IdmServiceConfiguration;
import java.net.URLEncoder;
import java.io.IOException;
import java.lang.reflect.Type;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import models.User;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

public class RestIdmService implements IdmService {

    private IdmServiceConfiguration config;
    private String IDM_URL;
    private String API_CLIENT_ID;
    private String API_CLIENT_SECRET;
    private String IDM_REQUEST_TYPE;;
    private String IDM_USERS_URI;
    private String IDM_TOKENS_URI;

    public RestIdmService()
        throws NoSuchAlgorithmException, KeyManagementException, IOException {

        config = new IdmServiceConfiguration();
        IDM_URL = config.getServerAddress();
        API_CLIENT_ID = config.getApiClientId();
        API_CLIENT_SECRET = config.getApiClientSecret();
        IDM_REQUEST_TYPE = config.getServerRequestType();
        IDM_USERS_URI = config.getUsersResourceUri();
        IDM_TOKENS_URI = config.getTokensResourceUri();
    }

    public boolean addUser(User user)  {

        String token = getTokenByClient();
        Map headers = new HashMap();
        headers.put("Authorization", String.format("token token=\"%s\"", token));

        boolean success;
        try {

            // build params
            List<NameValuePair> data = new ArrayList<NameValuePair>();
            data.add(new BasicNameValuePair("username", user.getUsername()));
            data.add(new BasicNameValuePair("password", user.getPassword()));
            data.add(new BasicNameValuePair("customerId", user.getCustomerNumber()));
            data.add(new BasicNameValuePair("personId", user.getPersonNumber()));
            data.add(new BasicNameValuePair("firstname", user.getFirstname()));
            data.add(new BasicNameValuePair("lastname", user.getLastname()));
            data.add(new BasicNameValuePair("middlename", user.getMiddlename()));
            data.add(new BasicNameValuePair("email", user.getEmail()));
            data.add(new BasicNameValuePair("secretQuestion", user.getSecretQuestion()));
            data.add(new BasicNameValuePair("secretAnswer", user.getSecretAnswer()));
            data.add(new BasicNameValuePair("preferredLang", user.getPreferredLang()));
            data.add(new BasicNameValuePair("timezone", user.getTimezone()));

            String url = IDM_URL + IDM_USERS_URI;
            HttpClientService httpClient = new HttpClientService();
            HttpResponse response = httpClient.sendPost(url, IDM_REQUEST_TYPE, headers, data);
            success = true;
        }
        catch (Exception ex){
            success = false;
        }
        return success;
    }

    public boolean deleteUser(String username) {

        String token = getTokenByClient();
        Map headers = new HashMap();
        headers.put("Authorization", String.format("token token=\"%s\"", token));

        boolean success;
        try {
            String url = IDM_URL + IDM_USERS_URI + "/" + username;
            HttpClientService httpClient = new HttpClientService();
            HttpResponse response = httpClient.sendDelete(url, headers);
            success = true;
        }
        catch (Exception ex){
            success = false;
        }

        return success;
    }

    public String getTokenByUsername(String username, String password) {

        List<NameValuePair> data = new ArrayList<NameValuePair>();
        data.add(new BasicNameValuePair("type", "username"));
        data.add(new BasicNameValuePair("client_id", API_CLIENT_ID));
        data.add(new BasicNameValuePair("client_secret", API_CLIENT_SECRET));
        data.add(new BasicNameValuePair("username", username));
        data.add(new BasicNameValuePair("password", password));

        // get token
        Map headers = new HashMap();
        String token;
        try {
            String url = IDM_URL + IDM_TOKENS_URI;
            HttpClientService httpClient = new HttpClientService();
            HttpResponse response = httpClient.sendPost(url, IDM_REQUEST_TYPE, headers, data);
            String responseBody = httpClient.getResponseBody(response);
            Map responseParams = getResponseParams(responseBody);

            token = (String)responseParams.get("access_token");
        }
        catch (Exception ex){
            token = null;
        }

        return token;
    }

    public User getUser(String username)
            throws HttpException {
        String token = getTokenByClient();
        Map headers = new HashMap();
        headers.put("Authorization", String.format("token token=\"%s\"", token));

        User user;
        try {
            String url = IDM_URL + IDM_USERS_URI + "/" + username;
            HttpClientService httpClient = new HttpClientService();
            HttpResponse response = httpClient.sendGet(url, headers);
            String responseBody = httpClient.getResponseBody(response);
            
            Gson gson = new Gson();
            user = gson.fromJson(responseBody, User.class);
        }
        catch (HttpException ex) {
            throw new HttpException("User exists");
        }
        catch (ResourceNotFoundException ex) {
            user = null;
        }
        catch (Exception ex) {
            user = null;
        }
        return user;
    }

    public List<User> getUsersByCustomerId(String customerId) {

        List<User> users = new ArrayList<User>();

        String token = getTokenByClient();
        Map headers = new HashMap();
        headers.put("Authorization", String.format("token token=\"%s\"", token));

        try {
            String url = IDM_URL + IDM_USERS_URI;
            HttpClientService httpClient = new HttpClientService();
            HttpResponse response = httpClient.sendGet(url, headers);
            String responseBody = httpClient.getResponseBody(response);
            if (responseBody.length() > 0) {

                Type listType = new TypeToken<List<User>>() {}.getType();
                Gson gson = new Gson();
                users = gson.fromJson(responseBody, listType);
            }
        }
        catch (Exception ex){
            ex.printStackTrace();
        }

        return users;
    }

    public boolean updateUser(User user) {
        String token = getTokenByClient();
        Map headers = new HashMap();
        headers.put("Authorization", String.format("token token=\"%s\"", token));

        boolean success;
        try {

            // build params
            List<NameValuePair> data = new ArrayList<NameValuePair>();
            data.add(new BasicNameValuePair("customerId", user.getCustomerNumber()));
            data.add(new BasicNameValuePair("firstname", user.getFirstname()));
            data.add(new BasicNameValuePair("lastname", user.getLastname()));
            data.add(new BasicNameValuePair("email", user.getEmail()));
            data.add(new BasicNameValuePair("middlename", user.getMiddlename()));
            data.add(new BasicNameValuePair("secretQuestion", URLEncoder.encode(user.getSecretQuestion(), "utf-8")));
            data.add(new BasicNameValuePair("secretAnswer", URLEncoder.encode(user.getSecretAnswer(), "utf-8")));
            data.add(new BasicNameValuePair("preferredLang", user.getPreferredLang()));
            data.add(new BasicNameValuePair("timezone", user.getTimezone()));

            String url = IDM_URL + IDM_USERS_URI + user.getUsername();
            HttpClientService httpClient = new HttpClientService();
            HttpResponse response = httpClient.sendPost(url,
                    IDM_REQUEST_TYPE, headers, data);
            success = true;
        }
        catch (Exception ex){
            success = false;
        }
        return success;
    }

    private String getTokenByClient() {

        List<NameValuePair> data = new ArrayList<NameValuePair>();
        data.add(new BasicNameValuePair("grant_type", "none"));
        data.add(new BasicNameValuePair("client_id", API_CLIENT_ID));
        data.add(new BasicNameValuePair("client_secret", API_CLIENT_SECRET));

        // get token
        Map headers = new HashMap();
        String token;
        try {
            String url = IDM_URL + IDM_TOKENS_URI;
            HttpClientService httpClient = new HttpClientService();
            HttpResponse response = httpClient.sendPost(url, IDM_REQUEST_TYPE, headers, data);
            String responseBody = httpClient.getResponseBody(response);
            Map responseParams = getResponseParams(responseBody);

            token = (String)responseParams.get("access_token");
        }
        catch (Exception ex){
            token = "";
        }

        return token;
    }

    private Map getResponseParams(String response) {

        Map responseParams = new HashMap();
        String[] responseParts = response.split("&");
        for(int i=0; i<responseParts.length; i++) {
            String[] keyvalPair = responseParts[i].split("=");
            if (keyvalPair.length == 2) {
                responseParams.put(keyvalPair[0], keyvalPair[1]);
            }
        }
        
        return responseParams;
    }
}
