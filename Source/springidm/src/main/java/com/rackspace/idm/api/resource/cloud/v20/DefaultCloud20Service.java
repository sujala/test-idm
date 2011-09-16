package com.rackspace.idm.api.resource.cloud.v20;

import org.openstack.docs.identity.api.v2.AuthenticationRequest;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 9/14/11
 * Time: 5:14 PM
 */
@Component
public class
        DefaultCloud20Service implements Cloud20Service{

    @Override
    public Response.ResponseBuilder authenticate(HttpHeaders httpHeaders, JAXBElement<AuthenticationRequest> authenticationRequest) throws IOException {
        //TODO write me
        throw new UnsupportedOperationException("not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.authenticate");
    }
}
