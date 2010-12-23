package library;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import javax.security.sasl.AuthenticationException;
import models.User;
import models.Token;

public class DefaultUserService implements UserService {

    public DefaultUserService() {}

    public User GetUser(String username)
    {
        User currentuser = User.find("LOWER(username) = ?", username.toLowerCase()).first();
        return currentuser;
    }

    public User AddUser(long accountId, String username, String password,
            String firstname, String lastname, String middlename,
            String email, String phone, String passwordHint)
        throws NoSuchAlgorithmException, UnsupportedEncodingException
    {
        // hash password
        HashMap hashedvalues = SecurityHelper.GetHashedPassword(password);
        String hashedPwd = hashedvalues.get("password").toString();
        String salt = hashedvalues.get("salt").toString();

        User currentuser = new User(accountId, username.toLowerCase(), hashedPwd, firstname, lastname,
                    middlename, email, phone, salt, passwordHint);

        currentuser.save();

        return currentuser;
    }

    public User UpdateUser(String username, String password,
            String firstname, String lastname, String middlename, String email, String phone)
            throws NoSuchAlgorithmException, UnsupportedEncodingException
    {
        User currentuser = this.GetUser(username);

        // hash password
        HashMap hashedvalues = SecurityHelper.GetHashedPassword(password);
        String hashedPwd = hashedvalues.get("password").toString();
        String salt = hashedvalues.get("salt").toString();

        currentuser.password = hashedPwd;
        currentuser.firstname = firstname;
        currentuser.lastname = lastname;
        currentuser.middlename = middlename;
        currentuser.email = email;
        currentuser.phone = phone;
        currentuser.salt = salt;

        currentuser.save();

        return currentuser;
    }

    public void SaveCloudLogin(String username, String cloudusername, String cloudpassword)
    {
        User currentuser = this.GetUser(username);
        currentuser.cloudcpmapped = true;
        currentuser.cloudusername = cloudusername;
        currentuser.cloudpassword = cloudpassword;

        currentuser.save();
    }

    public void SaveSecurityQuestionAnswer(String username, String securityQuestion, String securityAnswer, String securityImgPath)
    {
        User currentuser = this.GetUser(username);
        currentuser.SecurityQuestion = securityQuestion;
        currentuser.SecurityAnswer = securityAnswer;
        currentuser.SecurityImgPath = securityImgPath;

        currentuser.save();
    }

    public Token GetUserToken(String username, String password) 
            throws IOException, NoSuchAlgorithmException, AuthenticationException
    {

        User currentuser = this.GetUser(username);

        if (currentuser == null) {
            throw new AuthenticationException("invalid username");
        }

        byte[] bDigest = SecurityHelper.Base64ToByte(currentuser.password);
        byte[] bSalt = SecurityHelper.Base64ToByte(currentuser.salt);

        // Compute the new DIGEST
        byte[] proposedDigest = SecurityHelper.GetHash(SecurityHelper.ITERATION_NUMBER, password, bSalt);

        Boolean userAuthenticated = Arrays.equals(proposedDigest, bDigest);

        if (!userAuthenticated) {
            throw new AuthenticationException("invalid username or password");
        }

        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        Date now = new Date();
        String tokenInput = username.concat(dateFormat.format(now));
        String token = SecurityHelper.MakeSHA1Hash(tokenInput);

        Token.delete("relatedEntityId=?", username);  // clear all tokens for this user
        Token newToken = new Token(token, username, new Date());
        newToken.save();

        return newToken;
    }
}
