package com.rackspace.idm.util;

import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.IdmException;
import org.opensaml.Configuration;
import org.opensaml.saml2.core.Response;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.io.UnmarshallerFactory;
import org.opensaml.xml.io.UnmarshallingException;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

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

    public Response unmarshallResponse(final String responseString) {
        final ByteArrayInputStream is = new ByteArrayInputStream(responseString.getBytes());

        try {
            final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(true);
            final DocumentBuilder docBuilder = documentBuilderFactory.newDocumentBuilder();

            final Document document = docBuilder.parse(is);
            final Element element = document.getDocumentElement();

            final UnmarshallerFactory unmarshallerFactory = Configuration.getUnmarshallerFactory();
            final Unmarshaller unmarshaller = unmarshallerFactory.getUnmarshaller(element);
            final XMLObject responseXmlObj = unmarshaller.unmarshall(element);
            return (org.opensaml.saml2.core.Response) responseXmlObj;
        } catch (SAXException ex1) {
            throw new BadRequestException("Error parsing saml response", ex1);
        } catch (IllegalArgumentException ex2) {
            throw new BadRequestException("Invalid data in saml response", ex2);
        } catch (UnmarshallingException ex3) {
            throw new BadRequestException("Invalid format in saml response", ex3);
        } catch (Throwable t) {
            throw new IdmException("Error unmarshalling saml response", t);
        }
    }

}
