package com.rackspace.idm.api.resource.cloud.atomHopper;

import javax.xml.bind.annotation.*;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: Jun 4, 2012
 * Time: 3:47:58 PM
 * To change this template use File | Settings | File Templates.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AtomFeed", propOrder = {
    "user"
})
@XmlRootElement(name = "entry", namespace = "http://www.w3.org/2005/Atom")
public class AtomFeed {

    @XmlElement(required = true, namespace = "http://www.w3.org/2005/Atom")
    protected FeedUser user;

    public FeedUser getUser() {
        return user;
    }

    public void setUser(FeedUser value) {
        this.user = value;
    }

}
