package controllers;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import library.HttpClientService;
import play.mvc.Controller;
import models.User;
import org.apache.commons.lang.StringUtils;
import play.mvc.Before;
import com.google.gson.Gson;

import com.google.gson.reflect.TypeToken;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Type;
import java.util.List;
import javax.security.sasl.AuthenticationException;
import library.DefaultUserService;
import library.TokenService;
import library.UserService;
import models.AccessToken;
import models.Consumer;
import models.Provider;
import org.apache.commons.httpclient.*;
import org.apache.commons.io.IOUtils;
import play.Play;

public class Ui extends Controller {

    @Before(unless = {"login", "index", "createAccount", "createAccountSubmitted", "createAccountSuccess", "validateUsername"})
    public static void authenticate() {
        // cookies stuff
    }

    public static void login(String username, String password)
            throws IOException, NoSuchAlgorithmException
    {
        try {
            TokenService tokenService = new TokenService();
            AccessToken accessToken = tokenService.GetAccessToken(username, password);

            String tokenVal = accessToken.token;
            dashboard(username);
        }
        catch(Exception ex) {
            error(401, "Authentication failed");
        }
    }

    public static void index()
    {
        render();
    }

    public static void createAccount()
    {
        render();
    }

    public static void dashboard(String username)
    {
        UserService userService = new DefaultUserService();
        User user = userService.GetUser(username);
        Boolean cloudcpmapped = user.cloudcpmapped;

        Boolean hasSecurityQuestion = true;
        if (user.SecurityQuestion == null || user.SecurityQuestion.length() == 0) {
            hasSecurityQuestion = false;
        }

        render(cloudcpmapped, username, hasSecurityQuestion);
    }

    public static void cloudCredentialsSubmitted(String username, String cloudusername, String cloudpassword)
    {
        if (cloudusername.length() == 0 || cloudpassword.length() == 0) {
            error(400, "missing required parameters");
        }

        UserService userService = new DefaultUserService();
        userService.SaveCloudLogin(username, cloudusername, cloudpassword);
        mossoLogin(username);
    }

    public static void createAccountSubmitted()
        throws NoSuchAlgorithmException, UnsupportedEncodingException, HttpException
    {

        String oname = params.get("oname");
        String username = params.get("idname");
        String password = params.get("password");
        String pwdHint = params.get("passwordHint");
        long accountId = 123456;

        // send request to idm
        String url = "http://localhost:8080/idm/users";
        NameValuePair[] data = {
          new NameValuePair("accountid", String.valueOf(accountId)),
          new NameValuePair("username", username),
          new NameValuePair("password", password),
          new NameValuePair("firstname", ""),
          new NameValuePair("lastname", "")
        };

        HttpClientService httpService = new HttpClientService();
        httpService.sendPost(url, data);

        UserService userService = new DefaultUserService();
        userService.AddUser(accountId, username, password, "", "", "", "", "", pwdHint);
        createAccountSuccess();
    }

    public static void createAccountSuccess()
    {
        render();
    }

    public static void SecurityQuestions()
    {
        String username = params.get("username");
        render(username);
    }

    public static void validateUsername(String username)
            throws HttpException
    {
        if (StringUtils.isBlank(username)){
            error(400, "Missing required parameter: username");
        }

        Boolean identityExists = false;
        User currentuser = User.find("LOWER(username) =?", username.toLowerCase()).first();
        if (currentuser != null) {
            identityExists = true;
        }

        Boolean identityCharValid = true;
        Pattern identityCharPattern = Pattern.compile("[^A-Za-z0-9]");
        Matcher matcher = identityCharPattern.matcher(username);
        if (matcher.find()) {
            identityCharValid = false;
        }

        render(identityExists, identityCharValid);
    }

    public static void listProviders()
    {
        String url = "http://localhost:8080/idm/providers";
        HttpClientService httpService = new HttpClientService();

        String jsonResponse = httpService.sendGet(url);

        List<Provider> providers = null;
        if (jsonResponse.length() > 0) {
            Type listType = new TypeToken<List<Provider>>() {}.getType();
            Gson gson = new Gson();
            providers = gson.fromJson(jsonResponse, listType);
        }
        render(providers);
    }

    public static void registerProvider()
    {
        render();
    }

    public static void registerProviderSubmitted()
            throws HttpException
    {
        String oname = params.get("oname");
        String providername = params.get("providername");

        // send request to idm
        String url = "http://localhost:8080/idm/providers";
        NameValuePair[] data = {
          new NameValuePair("providername", providername),
          new NameValuePair("oname", oname)
        };

        HttpClientService httpService = new HttpClientService();
        httpService.sendPost(url, data);

        registerProviderSuccess(providername);
    }

    public static void registerProviderSuccess(String providername)
    {
        render(providername);
    }

    public static void registerConsumer(String providername)
    {
        render(providername);
    }

    public static void registerConsumerSubmitted()
            throws HttpException
    {
        String oname = params.get("oname");
        String consumername = params.get("consumername");
        String providername = params.get("providername");

        // send request to idm
        String url = "http://localhost:8080/idm/consumers";
        NameValuePair[] data = {
          new NameValuePair("consumername", consumername),
          new NameValuePair("oname", oname),
          new NameValuePair("providername", providername)
        };

        HttpClientService httpService = new HttpClientService();
        String jsonResponse = httpService.sendPost(url, data);

        Gson gson = new Gson();
        Consumer consumer = gson.fromJson(jsonResponse, Consumer.class);

        registerConsumerSuccess(providername, consumername, consumer.consumerKey, consumer.consumerSecret);
    }

    public static void registerConsumerSuccess(String providername,
            String consumername, String consumerKey, String consumerSecret)
    {
        render(providername, consumername, consumerKey, consumerSecret);
    }

    public static void testMossoLogin()
    {
        render();
    }

    public static void mossoLoginSubmitted(String username, String password)
            throws Exception, NoSuchAlgorithmException, UnsupportedEncodingException
    {
        try {
            TokenService tokenService = new TokenService();
            AccessToken accessToken = tokenService.GetAccessToken(username, password);

            accessToken.save();
            redirect("/ui/mossoDashboard?accessToken=" + accessToken.token);
        }
        catch (Exception ex)
        {
            error(401, "authentication failed");
        }
    }

    public static void mossoDashboard(String accessToken)
    {
        render(accessToken);
    }
    
    public static void mossoLogin(String username)
    {
        UserService userService = new DefaultUserService();
        User user = userService.GetUser(username);

        String cloudusername = user.cloudusername;
        String cloudpassword = user.cloudpassword;
        render(cloudusername, cloudpassword);
    }

    public static void SecretQuestionSubmitted(String username, String securityQuestion, String securityAnswer, String securityImgPath)
    {
        UserService userService = new DefaultUserService();
        userService.SaveSecurityQuestionAnswer(username, securityQuestion, securityAnswer, securityImgPath);
        dashboard(username);
    }

    public static void securityQImgUpload(File userfile)
            throws Exception
    {
        String imgdirpath = "/public/securityimages/";

        // delete existing images
        File imgdir = Play.getFile(imgdirpath);

        if( imgdir.exists() ) {
            File[] files = imgdir.listFiles();
            for(int i=0; i<files.length; i++) {
                files[i].delete();
            }
        }

        // save new image
        String filename = userfile.getName();
        FileInputStream is = new FileInputStream(userfile);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        String filepath = imgdirpath + filename;
        IOUtils.copy(is, new FileOutputStream(Play.getFile(filepath))); 
    }

    public static void testApiCall()
    {
        render();
    }

    public static void testApiSubmitted(String accessToken)
    {
        String resourceUrl = "http://localhost:9000/idm/testapi/images/21442";
        String queryString = String.format("?oauth_token=%s", accessToken);
        String url = resourceUrl + queryString;

        HttpClientService httpService = new HttpClientService();
        String responseStr = httpService.sendGet(url);

        render(responseStr);
    }

    public static void testApiHandler(String imgId, String oauth_token)
            throws HttpException
    {
        try {
            TokenService tokenService = new TokenService();
            tokenService.authenticateAccessToken(oauth_token);

            // do api stuff
            render(oauth_token);
        }
        catch (AuthenticationException ex)
        {
            error(401, "authentication failed");
        }
    }

    public static void forgotPassword()
    {
        render();
    }

    public static void getPasswordHint(String username)
    {
        UserService userService = new DefaultUserService();
        User user = userService.GetUser(username);
        String passwordHint = user.passwordHint;
        renderText(passwordHint);
    }

    public static void answerSecretQ(String username)
    {
        if (username.length() == 0) {
            redirect("/ui/forgotPassword");
        }
        render(username);
    }

    public static void getUserSecurityAttributes(String username)
    {
        UserService userService = new DefaultUserService();
        User user = userService.GetUser(username);
        
        renderJSON(String.format("{img_path:\"%s\", security_question: \"%s\"}", 
                user.SecurityImgPath,
                user.SecurityQuestion));
    }

    public static void answerSecretSubmitted(String username, String securityAnswer)
    {
        UserService userService = new DefaultUserService();
        User user = userService.GetUser(username);

        securityAnswer = securityAnswer.trim();
        String savedAnswer = user.SecurityAnswer.trim();

        if (savedAnswer.compareToIgnoreCase(securityAnswer) == 0) {
            resetPassword(username);
        }
        else {
            answerSecretQ(username);
        }
    }

    public static void resetPassword(String username)
    {
        render(username);
    }

    public static void resetPasswordSubmitted()
    {
        
    }
}