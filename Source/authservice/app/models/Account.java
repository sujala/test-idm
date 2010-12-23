package models;

import javax.persistence.*;
import play.db.jpa.*;

@Entity
public class Account extends Model {

    public String name;
    public String mailing_address;
    public String phone;
    public long primary_contact_id;

    public Account(String name, String mailing_address, String phone, long primary_contact_id) {
        this.name = name;
        this.mailing_address = mailing_address;
        this.phone = phone;
        this.primary_contact_id = primary_contact_id;
    }

}