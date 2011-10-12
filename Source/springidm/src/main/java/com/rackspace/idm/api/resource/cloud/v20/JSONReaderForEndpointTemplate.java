package com.rackspace.idm.api.resource.cloud.v20;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate;
import org.openstack.docs.identity.api.v2.VersionForService;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForEndpointTemplate implements
MessageBodyReader<EndpointTemplate> {
    @Override
    public boolean isReadable(Class<?> type, Type genericType,
        Annotation[] annotations, MediaType mediaType) {
        return type == EndpointTemplate.class;
    }

    @Override
    public EndpointTemplate readFrom(Class<EndpointTemplate> type,
        Type genericType, Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
        throws IOException, WebApplicationException {

        String jsonBody = IOUtils.toString(inputStream, "UTF-8");

        EndpointTemplate template = new EndpointTemplate();

        try {
            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);

            if (outer.containsKey("OS-KSCATALOG:endpointTemplate")) {
                JSONObject obj3;

                obj3 = (JSONObject) parser.parse(outer.get(
                    "OS-KSCATALOG:endpointTemplate").toString());
                
                Object id = obj3.get("id");
                Object adminURL = obj3.get("adminURL");
                Object internalURL = obj3.get("internalURL");
                Object name = obj3.get("name");
                Object publicURL = obj3.get("publicURL");
                Object serviceType = obj3.get("type");
                Object region = obj3.get("region");
                Object global = obj3.get("global");
                Object enabled = obj3.get("enabled");
                Object versionId = obj3.get("versionId");
                Object versionInfo = obj3.get("versionInfo");
                Object versionList = obj3.get("versionList");

                if (id != null) {
                    template.setId(Integer.parseInt(id.toString()));
                }
                if (adminURL != null) {
                    template.setAdminURL(adminURL.toString());
                }
                if (internalURL != null) {
                    template.setInternalURL(internalURL.toString());
                }
                if (name != null) {
                    template.setName(name.toString());
                }
                if (publicURL != null) {
                    template.setPublicURL(publicURL.toString());
                }
                if (serviceType != null) {
                    template.setType(serviceType.toString());
                }
                if (region != null) {
                    template.setRegion(region.toString());
                }
                if (global != null) {
                    template.setGlobal(Boolean.parseBoolean(global.toString()));
                }
                if (enabled != null) {
                    template.setEnabled(Boolean.parseBoolean(enabled.toString()));
                }
                if (versionId != null) {
                    VersionForService version = new VersionForService();
                    version.setId(versionId.toString());
                    
                    if (versionList != null) {
                        version.setList(versionList.toString());
                    }
                    if (versionInfo != null) {
                        version.setInfo(versionInfo.toString());
                    }
                    template.setVersion(version);
                }
            }
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return template;
    }
}
