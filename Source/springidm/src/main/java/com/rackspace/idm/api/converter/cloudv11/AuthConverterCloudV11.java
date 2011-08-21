package com.rackspace.idm.api.converter.cloudv11;

import java.util.List;

import com.rackspace.idm.cloud.jaxb.AuthData;
import com.rackspace.idm.cloud.jaxb.ServiceCatalog;
import com.rackspace.idm.domain.entity.CloudEndpoint;
import com.rackspace.idm.domain.entity.UserScopeAccess;

public class AuthConverterCloudV11 {
    private final TokenConverterCloudV11 tokenConverter;
    private final EndpointConverterCloudV11 endpointConverter;

    public AuthConverterCloudV11(TokenConverterCloudV11 tokenConverter, EndpointConverterCloudV11 endpointConverter) {
        this.tokenConverter = tokenConverter;
        this.endpointConverter = endpointConverter;
    }

    private final com.rackspace.idm.cloud.jaxb.ObjectFactory OBJ_FACTORY = new com.rackspace.idm.cloud.jaxb.ObjectFactory();

    public AuthData toCloudv11AuthDataJaxb(UserScopeAccess usa,
        List<CloudEndpoint> endpoints) {

        com.rackspace.idm.cloud.jaxb.AuthData auth = OBJ_FACTORY.createAuthData();

        auth.setToken(this.tokenConverter.toCloudv10TokenJaxb(usa));

        if (endpoints != null && endpoints.size() > 0) {
            ServiceCatalog catalog = this.endpointConverter
                .toServiceCatalog(endpoints);
            auth.setServiceCatalog(catalog);
        }

        return auth;
    }
}
