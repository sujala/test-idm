package com.rackspace.idm.domain.dozer.converters;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProviderFederationTypeEnum;
import org.dozer.DozerConverter;

public class IdentityProviderFederationTypeConverter extends DozerConverter<String, IdentityProviderFederationTypeEnum> {

    public IdentityProviderFederationTypeConverter() {
        super(String.class, IdentityProviderFederationTypeEnum.class);
    }

    @Override
    public IdentityProviderFederationTypeEnum convertTo(String source, IdentityProviderFederationTypeEnum destination) {
        if(source == null) {
            return null;
        }

        if(IdentityProviderFederationTypeEnum.DOMAIN.name().equals(source)) {
            return IdentityProviderFederationTypeEnum.DOMAIN;
        } else if(IdentityProviderFederationTypeEnum.RACKER.name().equals(source)) {
            return IdentityProviderFederationTypeEnum.RACKER;
        } else {
            throw new IllegalStateException(String.format("Invalid value for federation type: '%s'", source));
        }
    }

    @Override
    public String convertFrom(IdentityProviderFederationTypeEnum source, String destination) {
        return (source == null) ? null : source.toString();
    }
}
