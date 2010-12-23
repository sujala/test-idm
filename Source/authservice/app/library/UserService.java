package library;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import javax.security.sasl.AuthenticationException;
import models.Token;
import models.User;

public interface UserService
{
    public User GetUser(String username);

    public User AddUser(long accountId, String username, String password,
            String firstname, String lastname, String middlename,
            String email, String phone, String passwordHint)
        throws NoSuchAlgorithmException, UnsupportedEncodingException;

    public User UpdateUser(String username, String password,
            String firstname, String lastname, String middlename, String email, String phone)
            throws NoSuchAlgorithmException, UnsupportedEncodingException;

    public void SaveCloudLogin(String username, String cloudusername, String cloudpassword);

    public void SaveSecurityQuestionAnswer(String username, String securityQuestion, String securityAnswer, String securityImgPath);

    public Token GetUserToken(String username, String password) throws IOException, NoSuchAlgorithmException, AuthenticationException;
}