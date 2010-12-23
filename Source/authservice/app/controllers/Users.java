package controllers;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.security.sasl.AuthenticationException;
import org.apache.commons.lang.StringUtils;

import library.DefaultUserService;
import play.mvc.*;
import play.mvc.Http.Header;
import play.data.validation.*;
import models.User;
import models.Token;

@With(Security.class)
public class Users extends Controller {

    public static void list()
    {
        List<User> users = User.findAll();
        render(users);
    }

    public static void addUser()
            throws NoSuchAlgorithmException, UnsupportedEncodingException
    {
        String accountId = params.get("accountId");
        String username = params.get("username");
        String password = params.get("password");
        String firstname = params.get("firstname");
        String lastname = params.get("lastname");
        String middlename = params.get("middlename");
        String email = params.get("email");
        String phone = params.get("phone");
        String passwordHint = params.get("passwordHint");

        if (StringUtils.isBlank(accountId)){
            error(400, "Missing required parameter: accountId");
        }
        else if (StringUtils.isBlank(username)){
            error(400, "Missing required parameter: username");
        }
        else if (StringUtils.isBlank(password)) {
            error(400, "Missing required parameter: password");
        }

        DefaultUserService userService = new DefaultUserService();
        User currentuser = userService.GetUser(username);

        if (currentuser != null)
        {
            error(500, "A user with this username already exists.");
        }

        User newUser = userService.AddUser(Long.parseLong(accountId), username, password,
                firstname, lastname, middlename, email, phone, passwordHint);

        response.status = 201;
        Header location = new Header();
        location.name = "Location";
        location.values = new ArrayList<String>();
        location.values.add("/users/" + newUser.username);
        response.headers.put("Location", location);
        renderJSON("{\"uri\":" + "\"/users/" + newUser.username + "\"}");
    }

    public static void editUser(@Required String username)
        throws NoSuchAlgorithmException, UnsupportedEncodingException
    {
        DefaultUserService userService = new DefaultUserService();
        User currentuser = userService.GetUser(username);

        if (currentuser == null) {
             error(404, "User Not Found");
        }

        String accountId = params.get("accountId");
        String password = params.get("password");
        String firstname = params.get("firstname");
        String lastname = params.get("lastname");
        String middlename = params.get("middlename");
        String email = params.get("email");
        String phone = params.get("phone");

        if (StringUtils.isBlank(accountId)){
            error(400, "Missing required parameter: accountId");
        }
        else if (StringUtils.isBlank(password)) {
            error(400, "Missing required parameter: password");
        }

        User updatedUser = userService.UpdateUser(username, password, firstname, lastname, middlename, email, phone);

        response.status = 200;
        Header location = new Header();
        location.name = "Location";
        location.values = new ArrayList<String>();
        location.values.add("/users/" + updatedUser.username);
        response.headers.put("Location", location);
        renderJSON("{\"uri\":" + "\"/users/" + updatedUser.username + "\"}");
    }

    public static void getUserToken(@Required String username, @Required String password)
            throws NoSuchAlgorithmException, UnsupportedEncodingException, IOException
    {
        
        DefaultUserService userService = new DefaultUserService();

        try {
            Token token = userService.GetUserToken(username, password);
            renderText(token);
        }
        catch (AuthenticationException ex) {
            error(401, "Invalid username or password");
        }
    }

    @After
    static void log() {
        // log it
    }
}