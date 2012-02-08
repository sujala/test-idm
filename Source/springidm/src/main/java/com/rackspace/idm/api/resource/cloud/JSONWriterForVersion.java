package com.rackspace.idm.api.resource.cloud;

import com.rackspace.idm.JSONConstants;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.openstack.docs.common.api.v1.MediaTypeList;
import org.openstack.docs.common.api.v1.VersionChoice;
import org.w3._2005.atom.Link;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 2/3/12
 * Time: 4:16 PM
 */
@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriterForVersion implements MessageBodyWriter<JAXBElement<VersionChoice>> {

    @SuppressWarnings("unchecked")
    private JSONObject getVersionChoice(JAXBElement<VersionChoice> jaxbElement) {
        VersionChoice versionChoice = jaxbElement.getValue();

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
            JSONObject type_values = new JSONObject();
            type_values.put("values", typeArray);
            inner.put("media-types", type_values);
        }
        return outer;

    }


    @SuppressWarnings("unchecked")
    private static JSONArray getLinks(List<Object> any) {
        JSONArray linkArray = new JSONArray();
        for (Object o : any) {
            if (o instanceof JAXBElement) {
                Object elmType = ((JAXBElement<Object>) o).getValue();
                if (elmType instanceof Link) {
                    Link l = (Link) elmType;
                    JSONObject jlink = new JSONObject();
                    if (l.getRel() != null) {
                        jlink.put("rel", l.getRel().value());
                    }
                    if (l.getType() != null) {
                        jlink.put("type", l.getType());
                    }
                    jlink.put("href", l.getHref());
                    linkArray.add(jlink);
                }
            }
        }
        return linkArray;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == VersionChoice.class;
    }

    @Override
    public long getSize(JAXBElement<VersionChoice> versionChoice, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(JAXBElement<VersionChoice> jaxbElement, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                        MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        String jsonText = JSONValue.toJSONString(getVersionChoice(jaxbElement));
        entityStream.write(jsonText.getBytes(JSONConstants.UTF_8));
    }
}
