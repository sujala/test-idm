package com.rackspace.idm.util;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;

public class TemplateProcessor {
	
	public TemplateProcessor() throws IOException {
		Configuration cfg = new Configuration();
		cfg.setDirectoryForTemplateLoading(new File("/docs"));
		cfg.setObjectWrapper(new DefaultObjectWrapper());
	}
	
    public String getSubstituedOutput(String templateLine,
        Map<String, String> params) {
        if (templateLine == null || params == null) {
            throw new IllegalArgumentException("Null value(s) passed in");
        }

        String msg = templateLine;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            msg = StringUtils.replace(msg, "{{" + entry.getKey().trim() + "}}",
                entry.getValue());
        }

        return msg;
    }
}
