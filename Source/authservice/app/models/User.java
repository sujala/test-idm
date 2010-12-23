package models;
import javax.persistence.*;
import play.db.jpa.*;

@Entity
public class User extends Model {

    public long accountId;
    public String username;
    public String password;
    public String firstname;
    public String lastname;
    public String middlename;
    public String email;
    public String phone;
    public String salt;
    public String passwordHint;

    public Boolean cloudcpmapped;
    public String cloudusername;
    public String cloudpassword;

    public String SecurityQuestion;
    public String SecurityAnswer;
    public String SecurityImgPath;
    
    public User(long accountId, String username, String password,
            String firstname, String lastname, String middlename,
            String email, String phone, String salt, String passwordHint) {

        this.accountId = accountId;
        this.username = username;
        this.password = password;
        this.firstname = firstname;
        this.lastname = lastname;
        this.middlename = middlename;
        this.email = email;
        this.phone = phone;
        this.salt = salt;
        this.passwordHint = passwordHint;
    }
}