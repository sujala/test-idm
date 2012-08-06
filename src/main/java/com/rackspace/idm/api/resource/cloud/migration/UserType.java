//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.05.15 at 02:54:17 PM CDT 
//


package com.rackspace.idm.api.resource.cloud.migration;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java class for UserType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="UserType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="id" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="username" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="email" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="apiKey" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="password" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="secretQA" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="roles" type="{http://migration.api.rackspace.com/v1.0/MigrateUserResponse}RoleType" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="endpoints" type="{http://migration.api.rackspace.com/v1.0/MigrateUserResponse}EndpointType" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="groups" type="{http://migration.api.rackspace.com/v1.0/MigrateUserResponse}GroupType" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="comment" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="valid" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "UserType", propOrder = {
    "id",
    "mossoId",
    "nastId",
    "username",
    "email",
    "apiKey",
    "password",
    "secretQuestion",
    "secretAnswer",
    "roles",
    "endpoints",
    "groups"
})
public class UserType {

    @XmlElement
    protected String id;
    @XmlElement
    protected String mossoId;
    @XmlElement
    protected String nastId;
    @XmlElement
    protected String username;
    @XmlElement
    protected String email;
    @XmlElement
    protected String apiKey;
    @XmlElement
    protected String password;
    @XmlElement
    protected String secretQuestion;
    @XmlElement
    protected String secretAnswer;
    protected List<RoleType> roles;
    protected List<EndpointType> endpoints;
    protected List<GroupType> groups;
    @XmlAttribute
    protected String comment;
    @XmlAttribute
    protected Boolean valid;

    /**
     * Gets the value of the id property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setId(String value) {
        this.id = value;
    }

    /**
     * Gets the value of the mossoId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMossoId() {
        return mossoId;
    }

    /**
     * Sets the value of the mossoId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMossoId(String value) {
        this.mossoId = value;
    }

    /**
     * Gets the value of the nastId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNastId() {
        return nastId;
    }

    /**
     * Sets the value of the nastId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNastId(String value) {
        this.nastId = value;
    }

    /**
     * Gets the value of the username property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the value of the username property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUsername(String value) {
        this.username = value;
    }

    /**
     * Gets the value of the email property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the value of the email property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setEmail(String value) {
        this.email = value;
    }

    /**
     * Gets the value of the apiKey property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * Sets the value of the apiKey property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setApiKey(String value) {
        this.apiKey = value;
    }

    /**
     * Gets the value of the password property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the value of the password property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPassword(String value) {
        this.password = value;
    }

    /**
     * Gets the value of the secretQA property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSecretQuestion() {
        return secretQuestion;
    }

    /**
     * Sets the value of the secretQA property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSecretQuestion(String value) {
        this.secretQuestion = value;
    }

    /**
     * Gets the secretAnwser for this instance.
     *
     * @return The secretAnwser.
     */
    public String getSecretAnswer() {
        return this.secretAnswer;
    }

    /**
     * Sets the secretAnwser for this instance.
     *
     * @param secretAnwser The secretAnwser.
     */
    public void setSecretAnswer(String secretAnswer) {
        this.secretAnswer = secretAnswer;
    }

    /**
     * Gets the value of the roles property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the roles property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getRoles().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link RoleType }
     * 
     * 
     */
    public List<RoleType> getRoles() {
        if (roles == null) {
            roles = new ArrayList<RoleType>();
        }
        return this.roles;
    }

    /**
     * Gets the value of the endpoints property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the endpoints property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getEndpoints().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link EndpointType }
     * 
     * 
     */
    public List<EndpointType> getEndpoints() {
        if (endpoints == null) {
            endpoints = new ArrayList<EndpointType>();
        }
        return this.endpoints;
    }

    /**
     * Gets the value of the groups property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the groups property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getGroups().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link GroupType }
     * 
     * 
     */
    public List<GroupType> getGroups() {
        if (groups == null) {
            groups = new ArrayList<GroupType>();
        }
        return this.groups;
    }

    /**
     * Gets the value of the comment property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getComment() {
        return comment;
    }

    /**
     * Sets the value of the comment property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setComment(String value) {
        this.comment = value;
    }

    /**
     * Gets the value of the valid property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isValid() {
        return valid;
    }

    /**
     * Sets the value of the valid property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setValid(Boolean value) {
        this.valid = value;
    }

}
