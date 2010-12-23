package models;

import javax.persistence.*;
import play.db.jpa.*;

@Entity
public class AccessToken extends Model {

    public String token;
    public String username;
    public String tokenSecret;
    public String refreshToken;
    public int expireIn;

    public AccessToken(String token, String username, 
            String tokenSecret, String refreshToken,
            int expireIn) {
        this.token = token;
        this.username = username;
        this.tokenSecret = tokenSecret;
        this.refreshToken = refreshToken;
        this.expireIn = expireIn;
    }
}