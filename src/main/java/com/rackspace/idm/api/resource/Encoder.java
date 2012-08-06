package com.rackspace.idm.api.resource;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 8/31/11
 * Time: 11:37 AM
 */
public final class Encoder {

    private Encoder() {}

    public static String encode(String url) throws UnsupportedEncodingException {
        if(url != null) {
            return URLEncoder.encode(url, "UTF-8");
        }
        return url;
    }
}
