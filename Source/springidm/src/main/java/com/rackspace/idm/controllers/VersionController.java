package com.rackspace.idm.controllers;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.annotations.providers.jaxb.json.Mapped;
import org.jboss.resteasy.annotations.providers.jaxb.json.XmlNsMap;
import org.springframework.stereotype.Component;

import com.rackspace.idm.GlobalConstants;

@Path("/")
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@NoCache
@Component
public class VersionController {

    @GET
    @Mapped(namespaceMap = {@XmlNsMap(namespace = GlobalConstants.XML_NAMESPACE, jsonName = GlobalConstants.JSON_NAMESPACE)})
    public com.rackspace.idm.jaxb.Version getVersionInfo(
        @Context HttpServletResponse response) {
        com.rackspace.idm.jaxb.Version version = new com.rackspace.idm.jaxb.Version();
        version.setDocURL(GlobalConstants.DOC_URL);
        version.setId(GlobalConstants.VERSION);
        version.setStatus(Enum.valueOf(
            com.rackspace.idm.jaxb.VersionStatus.class,
            GlobalConstants.VERSION_STATUS.toUpperCase()));
        version.setWadl(GlobalConstants.WADL_URL);

        return version;
    }
}
