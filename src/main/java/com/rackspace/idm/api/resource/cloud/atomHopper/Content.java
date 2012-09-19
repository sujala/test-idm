package com.rackspace.idm.api.resource.cloud.atomHopper;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: Jun 4, 2012
 * Time: 3:46:25 PM
 * To change this template use File | Settings | File Templates.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Content")
public class Content {

    @XmlAttribute
    protected String type;

    public String getType() {
        return type;
    }

    public void setType(String value) {
        this.type = value;
    }
}