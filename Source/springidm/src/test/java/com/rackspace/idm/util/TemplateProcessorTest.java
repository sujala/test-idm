package com.rackspace.idm.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class TemplateProcessorTest {
    private static final String NEWLINE = System.getProperty("line.separator");

    @Test
    public void testGetSubstituedOutput() throws URISyntaxException,
        IOException {

        Map<String, String> params = new HashMap<String, String>();
        params.put("username", "bob.the.builder");
        params.put("serviceName", "RackFantastico");
        params.put("securityEmail", "dont.botherme@example.com");
        params.put("secNotifyEamilSubject", "Unrequested password change!");
        params.put("I shouldn't be here.", "nope.");

        URL templateUrl = getClass().getResource("/test_template.html");
        File templateFile = new File(templateUrl.toURI());
        BufferedReader reader = new BufferedReader(new FileReader(templateFile));
        String line = null;
        StringBuilder sb = new StringBuilder();
        TemplateProcessor tp = new TemplateProcessor();
        while ((line = reader.readLine()) != null) {
            sb.append(tp.getSubstituedOutput(line, params));
            sb.append(NEWLINE);
        }

        System.out.println(sb);
    }

}
