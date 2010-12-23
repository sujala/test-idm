package models;
import java.util.HashMap;
import java.util.Map;
import play.db.jpa.Model;

import java.net.URLDecoder;

public class User extends Model {

    String personNumber;
    String customerNumber;

    String username;
    String password;

    String firstname;
    String lastname;
    String middlename;
    String email;

    String secretQuestion;
    String secretAnswer;

    String preferredLang;
    String timezone;
    
    public User(String personNumber, String customerNumber, String username, String password,
            String firstname, String lastname, String middlename,
            String email, String secretQuestion, String secretAnswer,
            String preferredLang, String timezone) {

        this.personNumber = personNumber;
        this.customerNumber = customerNumber;

        this.username = username;
        this.password = password;

        this.firstname = firstname;
        this.lastname = lastname;
        this.middlename = middlename;
        this.email = email;

        this.secretQuestion = secretQuestion;
        this.secretAnswer = secretAnswer;
        this.preferredLang = preferredLang;
        this.timezone = timezone;
    }

    public String getPersonNumber() {
        return personNumber;
    }

    public void setPersonNumber(String value) {
        this.personNumber = value;
    }

    public String getCustomerNumber() {
        return customerNumber;
    }

    public void setCustomerNumber(String value) {
        this.customerNumber = value;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String value) {
        this.username = value;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String value) {
        this.password = value;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String value) {
        this.firstname = value;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String value) {
        this.lastname = value;
    }

    public String getMiddlename() {
        return middlename;
    }

    public void setMiddlename(String value) {
        this.middlename = value;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String value) {
        this.email = value;
    }

    public String getSecretQuestion() {
        try {
            return URLDecoder.decode(secretQuestion, "utf-8");
        }
        catch (Exception ex) {
            return secretQuestion;
        }
    }

    public void setSecretQuestion(String value) {
        this.secretQuestion = value;
    }

    public String getSecretAnswer() {
        try {
            return URLDecoder.decode(secretAnswer, "utf-8");
        }
        catch (Exception ex) {
            return secretAnswer;
        }
    }

    public void setSecretAnswer(String value) {
        this.secretAnswer = value;
    }

    public String getPreferredLang() {
        return preferredLang;
    }

    public void setPreferredLang(String value) {
        this.preferredLang = value;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String value) {
        this.timezone = value;
    }

    public static Map getNameParts(String fullname) {

        String[] nameParts = fullname.split(" ");
        String fname = nameParts[0];
        String lname = "";
        String mname = "";
        if (nameParts.length > 2) {
            lname = nameParts[nameParts.length - 1];
            for(int i=1; i < nameParts.length-1; i++) {
                mname += nameParts[i];
            }
        }
        else if (nameParts.length == 2) {
            lname = nameParts[1];
            mname = " ";
        }

        Map names = new HashMap();
        names.put("firstname", fname);
        names.put("lastname", lname);
        names.put("middlename", mname);

        return names;
    }
}