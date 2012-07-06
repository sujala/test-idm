package com.rackspace.idm.domain.dao.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;

import org.apache.commons.lang.StringUtils;

import com.rackspace.idm.domain.dao.ApiDocDao;

public class FileSystemApiDocRepository implements ApiDocDao {

    
    public String getContent(String path) {
        if (StringUtils.isBlank(path)) {
            return "";
        }

        InputStream stream = getClass().getResourceAsStream(path);
        if (stream == null) {
            return "";
        }

        try {
            return convertStreamToString(stream);
        } catch (IOException e) {
            return "";
        }
    }

    String convertStreamToString(InputStream is) throws IOException {
        if (is != null) {
            Writer writer = new StringWriter();

            char[] buffer = new char[1024];
            try {
                Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } finally {
                is.close();
            }
            return writer.toString();
        } else {
            return "";
        }
    }

}
