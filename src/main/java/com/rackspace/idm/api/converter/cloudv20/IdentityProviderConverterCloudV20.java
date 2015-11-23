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

        //map defaults to what we want to display to user
        if (provider.getFederationType() == null) {
            provider.setFederationType(IdentityProviderFederationTypeEnum.DOMAIN);
        }

        return provider;
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

        //map approvedDomainIds
        ApprovedDomainIds approvedDomainIdsWrapper = jaxbProvider.getApprovedDomainIds();
        if (approvedDomainIdsWrapper != null && CollectionUtils.isNotEmpty(approvedDomainIdsWrapper.getApprovedDomainId())) {
            if (provider.getApprovedDomainIds() == null) {
                provider.setApprovedDomainIds(new ArrayList<String>());
            }
            for (String approvedDomainId : approvedDomainIdsWrapper.getApprovedDomainId()) {
                provider.getApprovedDomainIds().add(approvedDomainId);
            }
        }

        return provider;
    }

    private X509Certificate certConverter(String pemEncoded) throws CertificateException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(Base64.decodeBase64(pemEncoded.getBytes(Charsets.UTF_8))));
    }
}
