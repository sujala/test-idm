package com.rackspace.idm.api.resource;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 8/31/11
 * Time: 11:37 AM
 */
public class Encoder {

    public static String encode(String url) throws UnsupportedEncodingException {
        return URLEncoder.encode(url, "UTF-8");
    }
}
