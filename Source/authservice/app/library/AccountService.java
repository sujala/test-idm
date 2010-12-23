package library;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import models.Account;
import models.User;

public class AccountService {

    public AccountService() {}

    public long AddAccount(String accountName, String accountMailingAddress, String accountphone,
            String username, String password,
            String firstname, String lastname, String middlename,
            String useremail, String userphone)
            throws NoSuchAlgorithmException, UnsupportedEncodingException {

        // create account
        Account account = new Account(accountName, accountMailingAddress, accountphone, 0);
        account.save();

        // add primary contact
        DefaultUserService userService = new DefaultUserService();
        User user = userService.AddUser(account.id, username, password, firstname, lastname, middlename, useremail, userphone,"");

        // save account with primary contact id
        account.primary_contact_id = user.id;
        account.save();

        return account.id;
    }
}
