package com.rackspace.idm.api.converter.cloudv11;

import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;

import com.rackspace.idm.domain.entity.CloudEndpoint;
import com.rackspace.idm.domain.entity.UserScopeAccess;
import com.rackspacecloud.docs.auth.api.v1.AuthData;
import com.rackspacecloud.docs.auth.api.v1.FullToken;
import com.rackspacecloud.docs.auth.api.v1.ServiceCatalog;

public class AuthConverterCloudV11 {
    private final Configuration config;
    private final TokenConverterCloudV11 tokenConverter;
    private final EndpointConverterCloudV11 endpointConverter;

    public AuthConverterCloudV11(Configuration config,
        TokenConverterCloudV11 tokenConverter,
        EndpointConverterCloudV11 endpointConverter) {
        this.config = config;
        this.tokenConverter = tokenConverter;
        this.endpointConverter = endpointConverter;
    }

    private final com.rackspacecloud.docs.auth.api.v1.ObjectFactory OBJ_FACTORY = new com.rackspacecloud.docs.auth.api.v1.ObjectFactory();

    public AuthData toCloudv11AuthDataJaxb(UserScopeAccess usa,
        List<CloudEndpoint> endpoints) {

        AuthData auth = OBJ_FACTORY.createAuthData();

        auth.setToken(this.tokenConverter.toCloudv11TokenJaxb(usa));

        if (endpoints != null && endpoints.size() > 0) {
            ServiceCatalog catalog = this.endpointConverter
                .toServiceCatalog(endpoints);
            auth.setServiceCatalog(catalog);
        }

        return auth;
    }

    public FullToken toCloudV11TokenJaxb(UserScopeAccess usa) {
        FullToken token = OBJ_FACTORY.createFullToken();

        token.setId(usa.getAccessTokenString());
        token.setUserId(usa.getUsername());
        token.setUserURL(String.format(getCloudUserRefString(), usa.getUsername()));

        try {
            if (usa.getAccessTokenExp() != null) {
                token.setCreated(DatatypeFactory.newInstance()
                    .newXMLGregorianCalendar(
                        new DateTime(usa.getAccessTokenExp()).minusSeconds(
                            getCloudAuthExpirationSeconds())
                            .toGregorianCalendar()));
                token.setExpires(DatatypeFactory.newInstance()
                    .newXMLGregorianCalendar(
                        new DateTime(usa.getAccessTokenExp())
                            .toGregorianCalendar()));
            }

        } catch (DatatypeConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return token;
    }

    private int getCloudAuthExpirationSeconds() {
        return config.getInt("token.cloudAuthExpirationSeconds");
    }
    
    private String getCloudUserRefString() {
        return config.getString("cloud.user.ref.string");
    }
}
