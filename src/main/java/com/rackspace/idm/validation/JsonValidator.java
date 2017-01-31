package com.rackspace.idm.validation;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

@Component
public class JsonValidator {

    /**
     * Validates that the provided string is valid JSON.
     *
     * If the provided string is null, blank, or empty then the string is not valid JSON.
     *
     * @param json
     * @return
     */
    public boolean isValidJson(String json) {
        if (StringUtils.isBlank(json)) {
            return false;
        }

        try {
            JSONParser jsonParser = new JSONParser();
            jsonParser.parse(new StringReader(json));
        } catch (ParseException | IOException ex) {
            return false;
        }

        return true;
    }

    /**
     * Validates that the provided JSON string is shorter than the provided size in kilobytes
     *
     * @param jsonString
     * @param sizeInKilobytes
     * @throws IllegalArgumentException if jsonString is null sizeInKilobytes is negative
     * @return
     */
    public boolean jsonStringDoesNotExceedSize(String jsonString, long sizeInKilobytes) {
        Validate.isTrue(jsonString != null);
        Validate.isTrue(sizeInKilobytes >= 0);

        byte [] jsonBytes = jsonString.getBytes(StandardCharsets.UTF_8);
        if(jsonBytes.length > sizeInKilobytes * 1024) {
            return false;
        }

        return true;
    }
}
