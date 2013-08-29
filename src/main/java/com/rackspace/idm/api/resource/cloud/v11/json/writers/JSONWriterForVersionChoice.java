package com.rackspace.idm.api.resource.cloud.v11.json.writers;

import com.rackspace.idm.JSONConstants;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.openstack.docs.common.api.v1.Extension;
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

import static com.rackspace.idm.api.resource.cloud.JsonWriterHelper.getLinks;

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 8/8/13
 * Time: 3:25 PM
 * To change this template use File | Settings | File Templates.
 */
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
        outputStream.write(jsonText.getBytes(JSONConstants.UTF_8));
    }

    JSONObject getVersionChoice(VersionChoice versionChoice) {

        JSONObject outer = new JSONObject();
        JSONObject inner = new JSONObject();

        outer.put("version", inner);

        inner.put("id", versionChoice.getId());
        if (versionChoice.getStatus() != null) {
            inner.put("status", versionChoice.getStatus().toString());
        }

        XMLGregorianCalendar updated = versionChoice.getUpdated();

        if (updated != null) {
            inner.put("updated", updated.toXMLFormat());
        }

        if (!versionChoice.getAny().isEmpty()) {
            JSONArray linkArray = getLinks(versionChoice.getAny());
            if (!linkArray.isEmpty()) {
                inner.put("links", linkArray);
            }
        }


        MediaTypeList mtl = versionChoice.getMediaTypes();
        if (mtl != null && !mtl.getMediaType().isEmpty()) {
            JSONArray typeArray = new JSONArray();
            for (org.openstack.docs.common.api.v1.MediaType mt : versionChoice.getMediaTypes().getMediaType()) {
                JSONObject jtype = new JSONObject();
                jtype.put("base", mt.getBase());
                jtype.put("type", mt.getType());
                typeArray.add(jtype);
            }
            JSONObject typeValues = new JSONObject();
            typeValues.put("values", typeArray);
            inner.put("media-types", typeValues);
        }
        return outer;

    }
}
