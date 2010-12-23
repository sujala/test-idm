package controllers;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import library.AccountService;
import play.mvc.*;
import play.mvc.Http.Header;
import models.Account;
import play.data.validation.*;


@With(Security.class)
public class Accounts extends Controller
{
    public static void list() {
        List<Account> accounts = Account.findAll();
        render(accounts);
    }

    public static void addAccount()
            throws NoSuchAlgorithmException, UnsupportedEncodingException
    {
        String accountName = params.get("accountName");
        String accountMailingAddress = params.get("accountMailingAddress");
        String accountphone = params.get("accountphone");
        
        String username = params.get("username");
        String password = params.get("password");
        String firstname = params.get("firstname");
        String lastname = params.get("lastname");
        String middlename = params.get("middlename");
        String useremail = params.get("email");
        String userphone = params.get("phone");

        if (StringUtils.isBlank(accountName)){
            error(400, "Missing required parameter: accountName");
        }
        else if (StringUtils.isBlank(username)){
            error(400, "Missing required parameter: username");
        }
        else if (StringUtils.isBlank(password)) {
            error(400, "Missing required parameter: password");
        }

        AccountService acctService = new AccountService();
        long accountId = acctService.AddAccount(accountName, accountMailingAddress, accountphone, 
                username, password, firstname, lastname, middlename, useremail, userphone);

        response.status = 201;
        Header location = new Header();
        location.name = "Location";
        location.values = new ArrayList<String>();
        location.values.add("/accounts/" + accountId);
        response.headers.put("Location", location);
        renderJSON("{\"uri\":" + "\"/accounts/" + accountId + "\"}");
    }
    
    public static void editAccount(@Required String accountId)
        throws NoSuchAlgorithmException, UnsupportedEncodingException, IOException
    {
        String accountName = params.get("accountName");
        String accountMailingAddress = params.get("accountMailingAddress");
        String accountphone = params.get("accountphone");

        if (StringUtils.isBlank(accountName)){
            error(400, "Missing required parameter: accountName");
        }
        else if (StringUtils.isBlank(accountId)){
            error(400, "Missing required parameter: accountId");
        }
        
        Account account = Account.findById(Long.parseLong(accountId));

        if (account == null){
            error(404, "Account not found");
        }

        account.name = accountName;
        account.mailing_address = accountMailingAddress;
        account.phone = accountphone;
        
        account.save();

        response.status = 200;
        Header location = new Header();
        location.name = "Location";
        location.values = new ArrayList<String>();
        location.values.add("/accounts/" + account.id);
        response.headers.put("Location", location);
        renderJSON("{\"uri\":" + "\"/accounts/" + account.id + "\"}");
    }

    @After
    static void log() {
        // log it
    }
}