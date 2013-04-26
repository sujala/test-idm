package com.rackspace.idm.util;

import com.rackspace.idm.domain.dao.impl.LdapPropertyRepository;
import com.rackspace.idm.domain.entity.Property;
import com.rackspace.idm.domain.service.impl.DefaultPropertiesService;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.*;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 4/19/13
 * Time: 10:35 AM
 * To change this template use File | Settings | File Templates.
 */
@Component
public class DefaultEncryptionPasswordSource implements EncryptionPasswordSource{
    @Autowired
    private Configuration config;

    @Autowired
    private DefaultPropertiesService propertiesService;

    private static String encryptionVersion = "encryptionVersionId";

    private HashMap<String, String> map;
    private String currentVersion;

    public DefaultEncryptionPasswordSource() {
        map = new LinkedHashMap<String, String>();
    }

    @PostConstruct
    public void init() throws IOException {
        currentVersion = propertiesService.getValue(encryptionVersion);
        readPasswords();
    }

    private void readPasswords() throws IOException {
        String[] stringArray = config.getStringArray("crypto.password");
        for (String item: stringArray) {
            String[] keyValue = item.split("\\|");
            map.put(keyValue[0], keyValue[1]);
        }
    }

    @Override
    public String getPassword() {
        return getPassword(currentVersion);
    }

    @Override
    public String getPassword(String version) {
        String password = map.get(version);
        if(StringUtils.isBlank(password)){
            String errMsg = String.format("%s is not a valid password version", version);
            throw new IllegalStateException(errMsg);
        }
        return password;
    }
}
