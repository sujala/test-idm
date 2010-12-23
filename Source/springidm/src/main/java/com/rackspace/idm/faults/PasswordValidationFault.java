package com.rackspace.idm.faults;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.jboss.resteasy.annotations.providers.jaxb.json.Mapped;
import org.jboss.resteasy.annotations.providers.jaxb.json.XmlNsMap;

import com.rackspace.idm.GlobalConstants;

/**
 * <p>
 * Java class for PasswordValidationFault complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within
 * this class.
 * 
 * <pre>
 * &lt;complexType name="PasswordValidationFault">
 *   &lt;complexContent>
 *     &lt;extension base="{http://docs.rackspacecloud.com/idm/api/v1.0}IdmFault">
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PasswordValidationFault")
@XmlRootElement(name = "passwordValidationFault")
@Mapped(namespaceMap = {@XmlNsMap(namespace = GlobalConstants.XML_NAMESPACE, jsonName = GlobalConstants.JSON_NAMESPACE)})
public class PasswordValidationFault extends IdmFault {

}
