package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.exception.BadRequestException;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate;
import org.openstack.docs.identity.api.v2.VersionForService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForEndpointTemplate implements
MessageBodyReader<EndpointTemplate> {
    private static Logger logger = LoggerFactory.getLogger(JSONReaderForEndpointTemplate.class);

    @Override
    public boolean isReadable(Class<?> type, Type genericType,
        Annotation[] annotations, MediaType mediaType) {
        return type == EndpointTemplate.class;
    }

    @Override
    public EndpointTemplate readFrom(Class<EndpointTemplate> type,
        Type genericType, Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
        throws IOException {

        String jsonBody = IOUtils.toString(inputStream, JSONConstants.UTF_8);

        EndpointTemplate template = getEndpointTemplateFromJSONString(jsonBody);

        return template;
    }
    
    public static EndpointTemplate getEndpointTemplateFromJSONString(String jsonBody) {
        EndpointTemplate template = new EndpointTemplate();

        try {
            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);

            if (outer.containsKey(JSONConstants.ENDPOINT_TEMPLATE)) {
                JSONObject obj3;

                obj3 = (JSONObject) parser.parse(outer.get(
                    JSONConstants.ENDPOINT_TEMPLATE).toString());
                
                Object id = obj3.get(JSONConstants.ID);
                Object adminURL = obj3.get(JSONConstants.ADMIN_URL);
                Object internalURL = obj3.get(JSONConstants.INTERNAL_URL);
                Object name = obj3.get(JSONConstants.NAME);
                Object publicURL = obj3.get(JSONConstants.PUBLIC_URL);
                Object serviceType = obj3.get(JSONConstants.TYPE);
                Object region = obj3.get(JSONConstants.REGION);
                Object global = obj3.get(JSONConstants.GLOBAL);
                Object enabled = obj3.get(JSONConstants.ENABLED);
                Object versionId = obj3.get(JSONConstants.VERSION_ID);
                Object versionInfo = obj3.get(JSONConstants.VERSION_INFO);
                Object versionList = obj3.get(JSONConstants.VERSION_LIST);

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
            logger.info(e.toString());
            throw new BadRequestException("Invalid JSON", e);
        }

        return template;
    }
}
