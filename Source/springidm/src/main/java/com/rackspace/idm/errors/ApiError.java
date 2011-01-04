package com.rackspace.idm.errors;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.rackspace.idm.GlobalConstants;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class ApiError {

    private String xmlns = GlobalConstants.API_NAMESPACE_LOCATION;
    private int code;
    private String message;
    private String details;

    public ApiError() {
    }

    public ApiError(int code, String message, String details) {
        this.code = code;
        this.message = message;
        this.details = details;
    }

    @XmlAttribute
    public String getXmlNamespace() {
        return xmlns;
    }

    @XmlElement
    public int getCode() {
        return code;
    }

    public void setStatusCode(int code) {
        this.code = code;
    }

    @XmlElement
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @XmlElement
    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    @Override
    public String toString() {
        return String.format("ApiError [code=%s, message=%s, details=%s]",
            code, message, details);
    }
}
