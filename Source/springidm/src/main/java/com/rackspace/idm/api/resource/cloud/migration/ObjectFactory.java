//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.05.15 at 02:54:17 PM CDT 
//


package com.rackspace.idm.api.resource.cloud.migration;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.rackspace.idm.api.resource.cloud.migration package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private static final QName RESPONSE_QNAME = new QName("http://migration.api.rackspace.com/v1.0/MigrateUserResponse", "response");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.rackspace.idm.api.resource.cloud.migration
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link MigrateUserResponseType }
     * 
     */
    public MigrateUserResponseType createMigrateUserResponseType() {
        return new MigrateUserResponseType();
    }

    /**
     * Create an instance of {@link UserType }
     * 
     */
    public UserType createUserType() {
        return new UserType();
    }

    /**
     * Create an instance of {@link RoleType }
     * 
     */
    public RoleType createRoleType() {
        return new RoleType();
    }

    /**
     * Create an instance of {@link GroupType }
     * 
     */
    public GroupType createGroupType() {
        return new GroupType();
    }

    /**
     * Create an instance of {@link EndpointType }
     * 
     */
    public EndpointType createEndpointType() {
        return new EndpointType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link MigrateUserResponseType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://migration.api.rackspace.com/v1.0/MigrateUserResponse", name = "response")
    public JAXBElement<MigrateUserResponseType> createResponse(MigrateUserResponseType value) {
        return new JAXBElement<MigrateUserResponseType>(RESPONSE_QNAME, MigrateUserResponseType.class, null, value);
    }

}
