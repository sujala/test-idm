package com.rackspace.idm.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PasswordRotationPolicy", propOrder = {
    "enabled",
    "duration"
})
@XmlRootElement(name = "passwordRotationPolicy")
public class PasswordRotationPolicy {
    
    @XmlAttribute(required = true)
    protected boolean enabled;
    
    @XmlAttribute(required = true)
    protected int duration;
    
    public void setDuration(int duration) {
        this.duration = duration;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean getEnabled() {
        return enabled;
    }
    
    public int getDuration() {
        return duration;
    }
}
