package com.rackspace.idm.api.converter.cloudv11;

import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;

import com.rackspace.idm.cloudv11.jaxb.AuthData;
import com.rackspace.idm.cloudv11.jaxb.FullToken;
import com.rackspace.idm.cloudv11.jaxb.ServiceCatalog;
import com.rackspace.idm.domain.entity.CloudEndpoint;
import com.rackspace.idm.domain.entity.UserScopeAccess;

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

    private final com.rackspace.idm.cloudv11.jaxb.ObjectFactory OBJ_FACTORY = new com.rackspace.idm.cloudv11.jaxb.ObjectFactory();

    public AuthData toCloudv11AuthDataJaxb(UserScopeAccess usa,
        List<CloudEndpoint> endpoints) {

        AuthData auth = OBJ_FACTORY.createAuthData();

        auth.setToken(this.tokenConverter.toCloudv10TokenJaxb(usa));

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
