package com.rackspace.idm.api.resource.cloud;

import com.rackspace.idm.JSONConstants;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openstack.docs.common.api.v1.Extension;
import org.w3._2005.atom.Link;

import javax.xml.bind.JAXBElement;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 8/28/13
 * Time: 5:15 PM
 * To change this template use File | Settings | File Templates.
 */
public class JsonWriterHelper {

    public static JSONArray getLinks(List<Object> any) {
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
                    if (l.getHref() != null) {
                        jlink.put("href", l.getHref());
                    }
                    linkArray.add(jlink);
                }
            }
        }
        return linkArray;
    }

    public static JSONObject getExtensionWithoutWrapper(Extension extension) {
        JSONObject outer = new JSONObject();

        outer.put(JSONConstants.NAME, extension.getName());
        outer.put(JSONConstants.NAMESPACE, extension.getNamespace());
        outer.put(JSONConstants.ALIAS, extension.getAlias());
        if (extension.getUpdated() != null) {
            outer.put(JSONConstants.UPDATED, extension.getUpdated().toString());
        }
        outer.put(JSONConstants.DESCRIPTION, extension.getDescription());

        if (extension.getAny().size() > 0) {
            JSONArray links = getLinks(extension.getAny());
            if (links.size() > 0) {
                outer.put(JSONConstants.LINKS, links);
            }
        }

        return outer;
    }
}
