package com.rackspace.idm.api.resource.cloud;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
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
}
