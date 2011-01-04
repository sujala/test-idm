package com.rackspace.idm.converters;

import com.rackspace.idm.entities.AuthData;
import com.rackspace.idm.jaxb.ObjectFactory;

public class AuthConverter {

    private UserConverter userConverter;
    private ClientConverter clientConverter;
    private PermissionConverter permissionConverter;
    private TokenConverter tokenConverter;

    protected ObjectFactory of = new ObjectFactory();

    public AuthConverter(TokenConverter tokenConverter,
        PermissionConverter permissionConverter,
        ClientConverter clientConverter, UserConverter userConverter) {
        this.tokenConverter = tokenConverter;
        this.permissionConverter = permissionConverter;
        this.clientConverter = clientConverter;
        this.userConverter = userConverter;
    }

    public com.rackspace.idm.jaxb.Auth toAuthDataJaxb(AuthData auth) {
        com.rackspace.idm.jaxb.Auth authJaxb = of.createAuth();

        if (auth.getAccessToken() != null) {
            authJaxb.setAccessToken(tokenConverter.toAccessTokenJaxb(auth
                .getAccessToken()));
        }

        if (auth.getRefreshToken() != null) {
            authJaxb.setRefreshToken(tokenConverter.toRefreshTokenJaxb(auth
                .getRefreshToken()));
        }

        if (auth.getClient() != null) {
            authJaxb.setClient(clientConverter
                .toClientJaxbWithoutPermissionsOrCredentials(auth.getClient()));
        }

        if (auth.getUser() != null) {
            authJaxb.setUser(userConverter.toUserWithOnlyRolesJaxb(auth
                .getUser()));
        }

        if (auth.getPermissions() != null) {
            authJaxb.setPermissions(permissionConverter.toPermissionListJaxb(auth
                .getPermissions()));
        }

        return authJaxb;
    }

}
