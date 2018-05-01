package com.rackspace.idm.api.resource.cloud;

import com.rackspace.idm.api.resource.cloud.v10.Cloud10VersionResource;
import com.rackspace.idm.api.resource.cloud.v11.Cloud11VersionResource;
import com.rackspace.idm.api.resource.cloud.v20.Cloud20VersionResource;
import com.rackspace.idm.api.serviceprofile.CloudContractDescriptionBuilder;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.event.ApiResourceType;
import com.rackspace.idm.event.IdentityApi;
import com.rackspace.idm.exception.IdmException;
import org.openstack.docs.common.api.v1.VersionChoiceList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;

/**
 * Cloud Auth API Versions
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class CloudVersionsResource {

    private final Cloud10VersionResource cloud10VersionResource;
    private final Cloud11VersionResource cloud11VersionResource;
    private final Cloud20VersionResource cloud20VersionResource;
    private final CloudContractDescriptionBuilder cloudContractDescriptionBuilder;
    private IdentityConfig identityConfig;

    private static final Class JAXBCONTEXT_VERSION_CHOICE_LIST_CONTEXT_PATH = VersionChoiceList.class;
    private static final JAXBContext JAXBCONTEXT_VERSION_CHOICE_LIST;

    static {
        try {
            JAXBCONTEXT_VERSION_CHOICE_LIST = JAXBContext.newInstance(JAXBCONTEXT_VERSION_CHOICE_LIST_CONTEXT_PATH);
        } catch (JAXBException e) {
            throw new IdmException("Error initializing JAXBContext for versionchoicelist", e);
        }
    }

    @Autowired
    public CloudVersionsResource(Cloud10VersionResource cloud10VersionResource,
                                 Cloud11VersionResource cloud11VersionResource,
                                 Cloud20VersionResource cloud20VersionResource,
                                 CloudContractDescriptionBuilder cloudContractDescriptionBuilder,
                                 IdentityConfig identityConfig) {
        this.cloud10VersionResource = cloud10VersionResource;
        this.cloud11VersionResource = cloud11VersionResource;
        this.cloud20VersionResource = cloud20VersionResource;
        this.cloudContractDescriptionBuilder = cloudContractDescriptionBuilder;
        this.identityConfig = identityConfig;
    }

    @IdentityApi(apiResourceType = ApiResourceType.PUBLIC, name = "Cloud Get internal versions xml")
    @Produces({MediaType.APPLICATION_XML})
    @GET
    public Response getInternalCloudVersionsInfo() throws JAXBException {
        JAXBContext context = JAXBCONTEXT_VERSION_CHOICE_LIST;
        if (!identityConfig.getReloadableConfig().reuseJaxbContext()) {
            //TODO causes memory leak...only left for backwards compatibility. Must be removed in future version.
            context = JAXBContext.newInstance(JAXBCONTEXT_VERSION_CHOICE_LIST_CONTEXT_PATH);
        }

        Unmarshaller unmarshaller = context.createUnmarshaller();
        final String responseXml = cloudContractDescriptionBuilder.buildInternalRootPage();
        JAXBElement<VersionChoiceList> versionChoice = (JAXBElement<VersionChoiceList>) unmarshaller.unmarshal(new StringReader(responseXml));
        return Response.ok(versionChoice.getValue()).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PUBLIC, name = "Cloud get internal versions json")
    @Produces({MediaType.APPLICATION_JSON})
    @GET
    public Response getInternalCloudVersionsInfoJson() throws JAXBException {
        Response response;
        if (!identityConfig.getReloadableConfig().returnJsonSpecificCloudVersionResource()) {
            response = getInternalCloudVersionsInfo(); //return the legacy way if feature disabled
        } else {
            String responseJson = cloudContractDescriptionBuilder.buildInternalRootPageJson();
            response = Response.ok(responseJson).build();
        }
        return response;
    }

    @Path("auth")
    public Cloud10VersionResource getCloud10AuthResource() {
        return cloud10VersionResource;
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
}
