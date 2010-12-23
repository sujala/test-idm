package models;

import javax.persistence.*;
import play.db.jpa.*;

@Entity
public class AuthKey extends Model {

    public String user;
    public String apiKey;
    public String secretKey;
    

    public AuthKey(String user, String apiKey, String secretKey) {
        this.user = user;
        this.apiKey = apiKey;
        this.secretKey = secretKey;
    }
}