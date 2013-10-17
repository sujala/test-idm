package com.rackspace.idm.util;

import org.opensaml.Configuration;
import org.opensaml.saml2.core.Response;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.io.UnmarshallerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;

/**
 * Created with IntelliJ IDEA.
 * User: itari.ighoroje
 * Date: 10/1/13
 * Time: 10:14 AM
 * To change this template use File | Settings | File Templates.
 */
@Component
public class SamlUnmarshaller {

    public Response unmarshallResponse(final String responseString) throws Throwable {
        ByteArrayInputStream is = new ByteArrayInputStream(responseString.getBytes());

        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        DocumentBuilder docBuilder = documentBuilderFactory.newDocumentBuilder();

        Document document = docBuilder.parse(is);
        Element element = document.getDocumentElement();

        UnmarshallerFactory unmarshallerFactory = Configuration.getUnmarshallerFactory();
        Unmarshaller unmarshaller = unmarshallerFactory.getUnmarshaller(element);
        XMLObject responseXmlObj = unmarshaller.unmarshall(element);

        return (org.opensaml.saml2.core.Response) responseXmlObj;
    }

}
