package com.rackspace.idm.api.converter.cloudv11;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Component;

import com.rackspace.idm.domain.entity.OpenstackEndpoint;
import com.rackspace.idm.domain.entity.UserScopeAccess;
import com.rackspacecloud.docs.auth.api.v1.AuthData;
import com.rackspacecloud.docs.auth.api.v1.FullToken;
import com.rackspacecloud.docs.auth.api.v1.ServiceCatalog;
import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.util.List;

@Component
public class AuthConverterCloudV11 {
    @Autowired
    private Configuration config;
    @Autowired
    private TokenConverterCloudV11 tokenConverter;
    @Autowired
    private EndpointConverterCloudV11 endpointConverter;
    private Logger logger = LoggerFactory.getLogger(AuthConverterCloudV11.class);

    private final com.rackspacecloud.docs.auth.api.v1.ObjectFactory objFactory = new com.rackspacecloud.docs.auth.api.v1.ObjectFactory();

    public AuthData toCloudv11AuthDataJaxb(UserScopeAccess usa, List<OpenstackEndpoint> endpoints) {

        AuthData auth = objFactory.createAuthData();

        auth.setToken(this.tokenConverter.toCloudv11TokenJaxb(usa));

        if (endpoints != null && endpoints.size() > 0) {
            ServiceCatalog catalog = this.endpointConverter.toServiceCatalog(endpoints);
            auth.setServiceCatalog(catalog);
        }

        return auth;
    }

    public FullToken toCloudV11TokenJaxb(UserScopeAccess usa, String requestUrl) {
        FullToken token = objFactory.createFullToken();

        token.setId(usa.getAccessTokenString());
        token.setUserId(usa.getUsername());
        token.setUserURL(requestUrl + "users/" + usa.getUsername());

        try {
            if (usa.getAccessTokenExp() != null) {
                token.setCreated(DatatypeFactory.newInstance()
                    .newXMLGregorianCalendar(new DateTime(usa.getAccessTokenExp()).minusSeconds(
                            getCloudAuthExpirationSeconds()).toGregorianCalendar()));
                token.setExpires(DatatypeFactory.newInstance()
                    .newXMLGregorianCalendar(new DateTime(usa.getAccessTokenExp()).toGregorianCalendar()));
            }

        } catch (DatatypeConfigurationException e) {
            logger.info("failed to create XMLGregorianCalendar: " + e.getMessage());
        }

        return token;
    }

    private int getCloudAuthExpirationSeconds() {
        return config.getInt("token.cloudAuthExpirationSeconds");
    }
}
