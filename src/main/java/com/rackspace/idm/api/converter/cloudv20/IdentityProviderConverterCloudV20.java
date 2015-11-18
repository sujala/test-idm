package com.rackspace.idm.api.converter.cloudv20;

import com.google.common.base.Charsets;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProvider;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProviders;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.PublicCertificate;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.PublicCertificates;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.exception.BadRequestException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections4.CollectionUtils;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

@Component
public class IdentityProviderConverterCloudV20 {
    @Autowired
    private Mapper mapper;

    @Autowired
    private JAXBObjectFactories objFactories;

    public IdentityProvider toIdentityProvider(com.rackspace.idm.domain.entity.IdentityProvider identityProvider) {
        com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProvider provider = mapper.map(identityProvider, IdentityProvider.class);

        if (identityProvider != null && CollectionUtils.isNotEmpty(identityProvider.getUserCertificates())) {
            PublicCertificates certWrapper = objFactories.getRackspaceIdentityExtRaxgaV1Factory().createPublicCertificates();
            for (byte[] cert : identityProvider.getUserCertificates()) {
                PublicCertificate publicCertificate = objFactories.getRackspaceIdentityExtRaxgaV1Factory().createPublicCertificate();
                publicCertificate.setPemEncoded(Base64.encodeBase64String(cert));
                certWrapper.getPublicCertificate().add(publicCertificate);
            }
            provider.setPublicCertificates(certWrapper);
        }

        return provider;
    }

    public IdentityProviders toIdentityProviderList(List<com.rackspace.idm.domain.entity.IdentityProvider> identityProviders) {

        IdentityProviders jaxbProviders = objFactories.getRackspaceIdentityExtRaxgaV1Factory().createIdentityProviders();

        for (com.rackspace.idm.domain.entity.IdentityProvider provider : identityProviders) {
            jaxbProviders.getIdentityProvider().add(toIdentityProvider(provider));
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

        return provider;
    }

    private X509Certificate certConverter(String pemEncoded) throws CertificateException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(Base64.decodeBase64(pemEncoded.getBytes(Charsets.UTF_8))));
    }
}
