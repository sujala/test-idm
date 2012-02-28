package com.rackspace.idm.api.resource.cloud;

import com.rackspace.idm.api.resource.cloud.v10.Cloud10VersionResource;
import com.rackspace.idm.api.resource.cloud.v11.Cloud11VersionResource;
import com.rackspace.idm.api.resource.cloud.v20.Cloud20VersionResource;
import com.rackspace.idm.api.serviceprofile.CloudContractDescriptionBuilder;
import org.apache.commons.configuration.Configuration;
import org.openstack.docs.common.api.v1.VersionChoiceList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.*;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.StringReader;

/**
 * Cloud Auth API Versions
 * 
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class CloudVersionsResource {
    
	private final CloudClient cloudClient;
    private final Cloud10VersionResource cloud10VersionResource;
    private final Cloud11VersionResource cloud11VersionResource;
    private final Cloud20VersionResource cloud20VersionResource;
    private final Configuration config;
    private final CloudContractDescriptionBuilder cloudContractDescriptionBuilder;

    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Context
    private UriInfo uriInfo;
    
    @Autowired
    public CloudVersionsResource(Cloud10VersionResource cloud10VersionResource,
    Cloud11VersionResource cloud11VersionResource,
    Cloud20VersionResource cloud20VersionResource, Configuration config,
    CloudContractDescriptionBuilder cloudContractDescriptionBuilder, CloudClient cloudClient) {
        this.cloud10VersionResource = cloud10VersionResource;
        this.cloud11VersionResource = cloud11VersionResource;
        this.cloud20VersionResource = cloud20VersionResource;
        this.config = config;
        this.cloudContractDescriptionBuilder = cloudContractDescriptionBuilder;
        this.cloudClient = cloudClient;
    }

    @GET
    public Response getInternalCloudVersionsInfo() throws JAXBException {
        final String responseXml = cloudContractDescriptionBuilder.buildInternalRootPage();
        JAXBContext context = JAXBContext.newInstance("org.openstack.docs.common.api.v1:org.w3._2005.atom");
        Unmarshaller unmarshaller = context.createUnmarshaller();
        JAXBElement<VersionChoiceList> versionChoice = (JAXBElement<VersionChoiceList>) unmarshaller.unmarshal(new StringReader(responseXml));
        return Response.ok(versionChoice).build();
    }

    public Response getPublicCloudVersionsInfo(@Context HttpHeaders httpHeaders) throws IOException {
    	return cloudClient.get(getCloudAuthBaseUrl(), httpHeaders).build();
    }
    
    @Path("v1.0")
    public Cloud10VersionResource getCloud10VersionResource() {
            return cloud10VersionResource;
    }
    
    @Path("v1.1")
    public Cloud11VersionResource getCloud11VersionResource() {
            return cloud11VersionResource;
    }
    
    @Path("v2.0")
    public Cloud20VersionResource getCloud20VersionResource() {
            return cloud20VersionResource;
    }
    
    private String getCloudAuthBaseUrl() {
        String url = config.getString("cloudAuth20url");
        
        // the url is in the form https://auth.staging.us.ccp.rackspace.net/v2.0/
        // we want to strip off the trailing /2.0/ to get the root resource
        return url.substring(0, url.indexOf("v2.0"));
    }
}
