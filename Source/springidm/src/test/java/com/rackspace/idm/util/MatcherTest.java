package com.rackspace.idm.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.Assert;

import org.junit.Test;


public class MatcherTest {

	@Test
	public void testMatcher() {
		String s = "LDAPConnectionPool(name='bind', serverSet=RoundRobinServerSet(servers={localhost:389,server:686}), maxConnections=1000)";
		Matcher matcher = Pattern.compile(".*\\{(.*)\\}.*").matcher(s);
		
		boolean found = matcher.find();
		String serverList="";
		
		if(found) {
			serverList = matcher.group(1);
			String[] servers = serverList.split(",");
			for (String server : servers) {
				String[] hostPort = server.split(":");
				Assert.assertNotNull(hostPort[1]);
			}
		}
		
		Assert.assertTrue(found);
		Assert.assertEquals(serverList, "localhost:389,server:686");
	}
}
