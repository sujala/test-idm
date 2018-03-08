package com.rackspace.idm.util;

import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.IdmException;
import org.apache.commons.codec.binary.StringUtils;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.LogoutResponse;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.io.Unmarshaller;
import org.opensaml.core.xml.io.UnmarshallerFactory;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;

@Component
public class SamlUnmarshaller {

    @Autowired
    private IdentityConfig identityConfig;

    public Response unmarshallResponse(final String responseString) {
        return unmarshallResponse(StringUtils.getBytesUtf8(responseString));
    }

    public Response unmarshallResponse(final byte[] samlResponseBytes) {
        final ByteArrayInputStream is = new ByteArrayInputStream(samlResponseBytes);

        XMLObject responseXmlObj;
        try {
            responseXmlObj = unmarshallSamlObject(is);
        } catch (SAXException ex1) {
            throw new BadRequestException("Error parsing saml response", ex1);
        } catch (IllegalArgumentException ex2) {
            throw new BadRequestException("Invalid data in saml response", ex2);
        } catch (UnmarshallingException ex3) {
            throw new BadRequestException("Invalid format in saml response", ex3);
        } catch (Throwable t) {
            throw new IdmException("Error unmarshalling saml response", t);
        }

        if (!(responseXmlObj instanceof Response)) {
            throw new BadRequestException("Invalid content for saml token request.");
        }

        return (org.opensaml.saml.saml2.core.Response) responseXmlObj;
    }

    private XMLObject unmarshallSamlObject(ByteArrayInputStream is) throws ParserConfigurationException, SAXException, IOException, UnmarshallingException {
        XMLObject responseXmlObj;
        //chances are this is thread safe and could just create a single time. Something to look at later...
        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        if (identityConfig.getReloadableConfig().ignoreCommentsWhenLoadingSaml()) {
            documentBuilderFactory.setIgnoringComments(true);
        }

        final DocumentBuilder docBuilder = documentBuilderFactory.newDocumentBuilder();

        final Document document = docBuilder.parse(is);
        final Element element = document.getDocumentElement();

        final UnmarshallerFactory unmarshallerFactory = XMLObjectProviderRegistrySupport.getUnmarshallerFactory();
        final Unmarshaller unmarshaller = unmarshallerFactory.getUnmarshaller(element);
        responseXmlObj = unmarshaller.unmarshall(element);
        return responseXmlObj;
    }

    public LogoutRequest unmarshallLogoutRequest(byte[] logoutRequestBytes) {
        ByteArrayInputStream is = new ByteArrayInputStream(logoutRequestBytes);

        XMLObject responseXmlObj;
        try {
            responseXmlObj = unmarshallSamlObject(is);
        } catch (SAXException sex) {
            throw new BadRequestException("Invalid saml logout request. Please check your syntax and try again.", sex);
        } catch (Exception t) {
            throw new IdmException("Error unmarshalling saml logout request", t);
        }

        if (!(responseXmlObj instanceof LogoutRequest)) {
            throw new BadRequestException("Invalid content for logout request.");
        }

        return (org.opensaml.saml.saml2.core.LogoutRequest) responseXmlObj;
    }

    public LogoutResponse unmarshallLogoutRespone(byte[] logoutResponseBytes) {
        ByteArrayInputStream is = new ByteArrayInputStream(logoutResponseBytes);

        XMLObject responseXmlObj;
        try {
            responseXmlObj = unmarshallSamlObject(is);
        } catch (SAXException sex) {
            throw new BadRequestException("Invalid saml logout response. Please check your syntax and try again.", sex);
        } catch (Exception t) {
            throw new IdmException("Error unmarshalling saml logout response", t);
        }

        if (!(responseXmlObj instanceof LogoutResponse)) {
            throw new BadRequestException("Invalid content for logout request.");
        }

        return (LogoutResponse) responseXmlObj;
    }

}
