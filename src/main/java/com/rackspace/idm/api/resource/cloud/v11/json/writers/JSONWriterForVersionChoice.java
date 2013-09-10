package com.rackspace.idm.api.resource.cloud.v11.json.writers;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.openstack.docs.common.api.v1.MediaTypeList;
import org.openstack.docs.common.api.v1.VersionChoice;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import static com.rackspace.idm.JSONConstants.*;
import static com.rackspace.idm.api.resource.cloud.JsonWriterHelper.getLinks;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriterForVersionChoice implements MessageBodyWriter<VersionChoice> {
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == VersionChoice.class;
    }

    @Override
    public long getSize(VersionChoice versionChoice, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(VersionChoice versionChoice, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream outputStream) throws IOException, WebApplicationException {
        String jsonText = JSONValue.toJSONString(getVersionChoice(versionChoice));
        outputStream.write(jsonText.getBytes(UTF_8));
    }

    JSONObject getVersionChoice(VersionChoice versionChoice) {

        JSONObject outer = new JSONObject();
        JSONObject inner = new JSONObject();

        outer.put(VERSION, inner);

        inner.put(ID, versionChoice.getId());
        if (versionChoice.getStatus() != null) {
            inner.put(STATUS, versionChoice.getStatus().toString());
        }

        XMLGregorianCalendar updated = versionChoice.getUpdated();

        if (updated != null) {
            inner.put(UPDATED, updated.toXMLFormat());
        }

        if (!versionChoice.getAny().isEmpty()) {
            JSONArray linkArray = getLinks(versionChoice.getAny());
            if (!linkArray.isEmpty()) {
                inner.put(LINKS, linkArray);
            }
        }


        MediaTypeList mtl = versionChoice.getMediaTypes();
        if (mtl != null && !mtl.getMediaType().isEmpty()) {
            JSONArray typeArray = new JSONArray();
            for (org.openstack.docs.common.api.v1.MediaType mt : versionChoice.getMediaTypes().getMediaType()) {
                JSONObject jtype = new JSONObject();
                jtype.put(BASE, mt.getBase());
                jtype.put(TYPE, mt.getType());
                typeArray.add(jtype);
            }
            JSONObject typeValues = new JSONObject();
            typeValues.put(VALUES, typeArray);
            inner.put(MEDIA_TYPES, typeValues);
        }
        return outer;

    }
}
