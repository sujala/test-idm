package com.rackspace.idm.util;

import java.util.Map;

import org.apache.commons.lang.StringUtils;

public class TemplateProcessor {
	
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
