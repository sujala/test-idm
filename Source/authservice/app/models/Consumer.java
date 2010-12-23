/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package models;

import javax.persistence.*;
import play.db.jpa.*;

@Entity
public class Consumer extends Model {

    public String name;
    public String oname;
    public String providername;
    public String consumerKey;
    public String consumerSecret;
}
