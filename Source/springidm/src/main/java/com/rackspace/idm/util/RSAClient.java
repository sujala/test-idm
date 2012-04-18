package com.rackspace.idm.util;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tinyradius.util.RadiusClient;

/**
 * Created by IntelliJ IDEA. User: Hector Date: 3/1/12 Time: 2:10 PM
 */
@Component
public class RSAClient {

	private static Logger logger = LoggerFactory.getLogger(RSAClient.class);

	@Autowired
	private Configuration config;
    
	public boolean authenticate(String userID, String passCode) {
		try {
			String rsaHost = config.getString("rsa.host");
			String rsaSecret = config.getString("rsa.sharedSecret");
			logger.debug("rsa host: " + rsaHost);
			
			RadiusClient client = new RadiusClient(rsaHost, rsaSecret);
			return client.authenticate(userID, passCode);
		} catch (Throwable t) {
			logger.info("error authentication racker with rsa credentials: {}", t.getMessage());
			return false;
		}
	}
}
