package models;

import javax.persistence.*;
import play.db.jpa.*;

@Entity
public class Provider extends Model {

    public String name;
    public String oname;

    public Provider(String name, String oname) {
        this.name = name;
        this.oname = oname;
    }

}