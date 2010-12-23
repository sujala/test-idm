package models;

import java.util.Date;
import javax.persistence.*;
import play.db.jpa.*;

@Entity
public class Token extends Model {

    public String value;
    public String relatedEntityId;
    public Date lastModified;

    public Token(String value, String relatedEntityId, Date lastModified) {
        this.value = value;
        this.relatedEntityId = relatedEntityId;
        this.lastModified = lastModified;
    }

}