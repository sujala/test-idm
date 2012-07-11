package com.rackspace.idm.api.resource.cloud.atomHopper;

import javax.xml.bind.annotation.*;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: Jun 4, 2012
 * Time: 3:46:25 PM
 * To change this template use File | Settings | File Templates.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "FeedUser")
public class FeedUser {

    @XmlAttribute
    protected String username;
    @XmlAttribute
    protected String id;
    @XmlAttribute
    protected String displayName;
    @XmlAttribute
    protected String migrationStatus;

    public String getUsername() {
        return username;
    }

    public void setUsername(String value) {
        this.username = value;
    }

    public String getId() {
        return id;
    }

    public void setId(String value) {
        this.id = value;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String value) {
        this.displayName = value;
    }

    public void setMigrationStatus(String value){
        this.migrationStatus = value;
    }

}