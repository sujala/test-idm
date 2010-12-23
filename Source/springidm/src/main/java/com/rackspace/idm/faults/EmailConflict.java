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
 * Java class for EmailConflictFault complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within
 * this class.
 * 
 * <pre>
 * &lt;complexType name="EmailConflictFault">
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
@XmlType(name = "EmailConflictFault")
@XmlRootElement(name = "emailConflict")
@Mapped(namespaceMap = {@XmlNsMap(namespace = GlobalConstants.XML_NAMESPACE, jsonName = GlobalConstants.JSON_NAMESPACE)})
public class EmailConflict extends IdmFault {

}
