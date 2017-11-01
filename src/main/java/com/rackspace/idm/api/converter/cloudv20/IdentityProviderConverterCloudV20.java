package com.rackspace.idm.api.converter.cloudv20;

import com.google.common.base.Charsets;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.*;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.exception.BadRequestException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.dozer.Mapper;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.Marshaller;
import org.opensaml.core.xml.io.MarshallerFactory;
import org.opensaml.core.xml.io.UnmarshallerFactory;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml.saml2.metadata.KeyDescriptor;
import org.opensaml.saml.saml2.metadata.SingleSignOnService;
import org.opensaml.security.credential.UsageType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;

import static org.opensaml.saml.common.xml.SAMLConstants.SAML20P_NS;
import static org.opensaml.saml.common.xml.SAMLConstants.SAML2_REDIRECT_BINDING_URI;

@Component
public class IdentityProviderConverterCloudV20 {
    @Autowired
    private Mapper mapper;

    @Autowired
    private JAXBObjectFactories objFactories;

    public static final int METADATA_IDP_MAX_NAME_SIZE = 29;

    // NOTE: Metadata is not return as part of the IDP object.
    public IdentityProvider toIdentityProvider(com.rackspace.idm.domain.entity.IdentityProvider identityProvider) {
        com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProvider provider = mapper.map(identityProvider, IdentityProvider.class);

        if (identityProvider != null && CollectionUtils.isNotEmpty(identityProvider.getUserCertificates())) {
            PublicCertificates certWrapper = objFactories.getRackspaceIdentityExtRaxgaV1Factory().createPublicCertificates();
            for (byte[] cert : identityProvider.getUserCertificates()) {
                PublicCertificate publicCertificate = objFactories.getRackspaceIdentityExtRaxgaV1Factory().createPublicCertificate();
                publicCertificate.setPemEncoded(Base64.encodeBase64String(cert));
                publicCertificate.setId(DigestUtils.sha1Hex(cert));
                certWrapper.getPublicCertificate().add(publicCertificate);
            }
            provider.setPublicCertificates(certWrapper);
        }

        //map approvedDomainIds
        if (CollectionUtils.isNotEmpty(identityProvider.getApprovedDomainIds())) {
            ApprovedDomainIds approvedDomainIds = objFactories.getRackspaceIdentityExtRaxgaV1Factory().createApprovedDomainIds();
            for (String domainId : identityProvider.getApprovedDomainIds()) {
                approvedDomainIds.getApprovedDomainId().add(domainId);
            }
            provider.setApprovedDomainIds(approvedDomainIds);
        }

        // Map emailDomains
        if (CollectionUtils.isNotEmpty(identityProvider.getEmailDomains())) {
            EmailDomains emailDomains = objFactories.getRackspaceIdentityExtRaxgaV1Factory().createEmailDomains();
            for (String emailDomain : identityProvider.getEmailDomains()) {
                emailDomains.getEmailDomain().add(emailDomain);
            }
            provider.setEmailDomains(emailDomains);
        }

        //map defaults to what we want to display to user
        if (provider.getFederationType() == null) {
            provider.setFederationType(IdentityProviderFederationTypeEnum.DOMAIN);
        }

        return provider;
    }

    public IdentityProvider toIdentityProvider(byte[] metadata, String domainId) {
        Document xmlDocument = getXMLDocument(metadata);
        EntityDescriptor entityDescriptor = getEntityDescriptor(xmlDocument.getDocumentElement());
        if (entityDescriptor == null) {
            throw new BadRequestException("Invalid XML metadata file.");
        }

        // IDP cannot be created without a IDPSSODescriptor
        IDPSSODescriptor idpSSODescriptor = entityDescriptor.getIDPSSODescriptor(SAML20P_NS);
        if (idpSSODescriptor == null) {
            String errMsg = String.format("Invalid XML metadata: %s is a required element.",
                                          IDPSSODescriptor.DEFAULT_ELEMENT_LOCAL_NAME);
            throw new BadRequestException(errMsg);
        }

        IdentityProvider identityProvider = new IdentityProvider();
        identityProvider.setIssuer(entityDescriptor.getEntityID());
        identityProvider.setMetadata(convertEntityDescriptorToString(entityDescriptor));
        // Set IDP name to the first 29 characters of the caller's domainId
        identityProvider.setName(StringUtils.substring(domainId, 0, METADATA_IDP_MAX_NAME_SIZE));
        identityProvider.setFederationType(IdentityProviderFederationTypeEnum.DOMAIN);
        ApprovedDomainIds approvedDomainIds = new ApprovedDomainIds();
        approvedDomainIds.getApprovedDomainId().add(domainId);
        identityProvider.setApprovedDomainIds(approvedDomainIds);

        // Set description to organization name if present, else set it to the domainId of caller.
        if (entityDescriptor.getOrganization() != null && entityDescriptor.getOrganization().getDisplayNames() != null) {
            identityProvider.setDescription(entityDescriptor.getOrganization().getOrganizationNames().get(0).getValue());
        } else {
            identityProvider.setDescription(domainId);
        }

        // Set authenticationUrl based on SingleSignOnService using binding HTTP-Redirect
        for (SingleSignOnService ssos : idpSSODescriptor.getSingleSignOnServices()) {
            if (ssos.getBinding().equals(SAML2_REDIRECT_BINDING_URI)) {
                identityProvider.setAuthenticationUrl(ssos.getLocation());
                break;
            }
        }

        // Set publicCertificates
        PublicCertificates publicCertificates = objFactories.getRackspaceIdentityExtRaxgaV1Factory().createPublicCertificates();
        for (KeyDescriptor keyDescriptor : idpSSODescriptor.getKeyDescriptors()) {
            if (keyDescriptor.getUse() == UsageType.SIGNING) {
                for (org.opensaml.xmlsec.signature.X509Certificate x509Certificate : keyDescriptor.getKeyInfo().getX509Datas().get(0).getX509Certificates()) {
                    PublicCertificate publicCertificate = new PublicCertificate();
                    publicCertificate.setPemEncoded(x509Certificate.getValue());
                    publicCertificates.getPublicCertificate().add(publicCertificate);
                }
            }
        }
        identityProvider.setPublicCertificates(publicCertificates);

        return identityProvider;
    }

    public Document getXMLDocument(byte[] data) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document metadataDoc = builder.parse(new ByteArrayInputStream(data));
            return metadataDoc;
        } catch (SAXException | ParserConfigurationException | IOException e) {
            String errMsg = "Invalid XML";
            throw new BadRequestException(errMsg, e);
        }
    }

    public EntityDescriptor getEntityDescriptor(Element element) {
        // Parse XML metadata file
        try {
            // Get apropriate unmarshaller
            UnmarshallerFactory unmarshallerFactory = XMLObjectProviderRegistrySupport.getUnmarshallerFactory();
            org.opensaml.core.xml.io.Unmarshaller unmarshaller = unmarshallerFactory.getUnmarshaller(element);

            if (unmarshaller == null) {
                return null;
            }

            // Unmarshall using the document root element EntityDescriptor
            return (EntityDescriptor) unmarshaller.unmarshall(element);
        } catch (UnmarshallingException e) {
            String errMsg = "Invalid XML";
            throw new BadRequestException(errMsg, e);
        }
    }

    private String convertEntityDescriptorToString(EntityDescriptor entityDescriptor) {
        try {
            // Get apropriate unmarshaller
            MarshallerFactory marshallerFactory = XMLObjectProviderRegistrySupport.getMarshallerFactory();
            Marshaller marshaller = marshallerFactory.getMarshaller(entityDescriptor);

            // convert EntityDescriptor to XML document
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.newDocument();
            marshaller.marshall(entityDescriptor, document);

            // Convert XML document to string
            StringWriter sw = new StringWriter();
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.transform(new DOMSource(document), new StreamResult(sw));
            return sw.toString();
        } catch (Exception ex) {
            throw new RuntimeException("Error converting EntityDescriptor to String", ex);
        }
    }

    public IdentityProviders toIdentityProviderList(List<com.rackspace.idm.domain.entity.IdentityProvider> identityProviders) {

        IdentityProviders jaxbProviders = objFactories.getRackspaceIdentityExtRaxgaV1Factory().createIdentityProviders();

        for (com.rackspace.idm.domain.entity.IdentityProvider provider : identityProviders) {
            IdentityProvider jaxbProvider = toIdentityProvider(provider);
            jaxbProvider.setPublicCertificates(null); //null out certs in list
            jaxbProviders.getIdentityProvider().add(jaxbProvider);
        }

        return jaxbProviders;
    }

    public com.rackspace.idm.domain.entity.IdentityProvider fromIdentityProvider(
            IdentityProvider jaxbProvider) {
        com.rackspace.idm.domain.entity.IdentityProvider provider = mapper.map(jaxbProvider, com.rackspace.idm.domain.entity.IdentityProvider.class);

        //map public certs
        PublicCertificates certWrapper = jaxbProvider.getPublicCertificates();
        if (certWrapper != null && CollectionUtils.isNotEmpty(certWrapper.getPublicCertificate())) {
            for (int i=0; i<certWrapper.getPublicCertificate().size(); i++) {
                PublicCertificate publicCertificate = certWrapper.getPublicCertificate().get(i);
                String pemEncodedCert = publicCertificate.getPemEncoded();
                try {
                    provider.addUserCertificate(certConverter(pemEncodedCert));
                } catch (CertificateException e) {
                    throw new BadRequestException(String.format("Invalid certificate at index '%d'", i), e);
                }
            }
        }

        //map approvedDomainIds - only accepting unique vals
        Set<String> domainIdSet = new LinkedHashSet<String>();
        ApprovedDomainIds approvedDomainIdsWrapper = jaxbProvider.getApprovedDomainIds();
        if (approvedDomainIdsWrapper != null && CollectionUtils.isNotEmpty(approvedDomainIdsWrapper.getApprovedDomainId())) {
            for (String approvedDomainId : approvedDomainIdsWrapper.getApprovedDomainId()) {
                domainIdSet.add(approvedDomainId);
            }
            if (provider.getApprovedDomainIds() == null) {
                provider.setApprovedDomainIds(new ArrayList<String>());
            }
            provider.getApprovedDomainIds().addAll(domainIdSet);
        }

        // Map emailDomains - only accepting unique values
        Set<String> emailDomainsSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        EmailDomains emailDomainsWrapper = jaxbProvider.getEmailDomains();
        if (emailDomainsWrapper != null && CollectionUtils.isNotEmpty(emailDomainsWrapper.getEmailDomain())) {
            emailDomainsSet.addAll(emailDomainsWrapper.getEmailDomain());
            if (provider.getEmailDomains() == null) {
                provider.setEmailDomains(new ArrayList<String>());
            }
            provider.getEmailDomains().addAll(emailDomainsSet);
        }

        //convert empty strings to nulls since they can't be persisted to LDAP
        if (StringUtils.isWhitespace(jaxbProvider.getDescription())) {
            provider.setDescription(null);
        }

        if (jaxbProvider.getMetadata() != null) {
            provider.setXmlMetadata(jaxbProvider.getMetadata().getBytes());
        }

        return provider;
    }

    private X509Certificate certConverter(String pemEncoded) throws CertificateException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(Base64.decodeBase64(pemEncoded.getBytes(Charsets.UTF_8))));
    }
}
