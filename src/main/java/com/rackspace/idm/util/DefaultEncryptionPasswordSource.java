package com.rackspace.idm.util;

import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

    private HashMap<Integer, String> map;
    private String filename;
    private Integer latestVersion;

    public DefaultEncryptionPasswordSource() {
        map = new LinkedHashMap<Integer, String>();
    }

    public void init() throws IOException {
        latestVersion = 0;
        map.put(map.size(), this.config.getString("crypto.password"));
        filename = config.getString("crypto.password.file");

        File file = new File(filename);
        if(file.exists()){
            FileReader fileReader = new FileReader(file);
            BufferedReader br = new BufferedReader(fileReader);
            String password;
            while((password = br.readLine()) != null){
                if(password.trim().length() > 0){
                    String[] parts = password.split("\\|");

                    final Integer version = Integer.parseInt(parts[0]);
                    map.put(version, parts[1]);
                    latestVersion = Math.max(latestVersion,version);
                }
            }
            fileReader.close();
        }
    }

    @Override
    public String getPassword() {
        return map.get(latestVersion);
    }

    @Override
    public String getPassword(Integer version) {
        return map.get(version);
    }

    @Override
    public void setPassword(String password) throws IOException {
        map.put(latestVersion+1,password);

        latestVersion++;

        FileWriter fileWriter = new FileWriter(new File(filename));
        fileWriter.append(password);
        fileWriter.close();
    }
}
