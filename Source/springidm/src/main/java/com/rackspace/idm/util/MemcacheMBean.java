package com.rackspace.idm.util;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.spy.memcached.MemcachedClient;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;

@ManagedResource
public class MemcacheMBean {
	 @Autowired
    private MemcachedClient memcached;
	 
	@ManagedAttribute
	public List<String> getAvailableServers() {
		List<String> list = new ArrayList<String>();
		Map<SocketAddress, Map<String, String>> stats = memcached.getStats();
		Collection<SocketAddress> servers = memcached.getAvailableServers();
		for (SocketAddress socketAddress : servers) {
			list.add(socketAddress.toString());
		}
		return list;
	}
	
	@ManagedAttribute
	public List<String> getUnavailableServers() {
		List<String> list = new ArrayList<String>();
		Map<SocketAddress, Map<String, String>> stats = memcached.getStats();
		Collection<SocketAddress> servers = memcached.getUnavailableServers();
		for (SocketAddress socketAddress : servers) {
			list.add(socketAddress.toString());
		}
		return list;
	}
	
	@ManagedAttribute
    public Map<String,String> getServerConnectionStatus() {
      
	    Map<String,String> result = new HashMap<String, String>();
	    
	    List<String> availableServerList = getAvailableServers();
	    List<String> unavailableServerList = getUnavailableServers();
	    
	    for(String server : availableServerList) {
	        server = server.replaceAll("/", "");
	        result.put(server, "up");
	    }
	    
	    for(String server : unavailableServerList) {
	        server = server.replaceAll("/", "");
	        result.put(server, "down");
	    }
	    
	    return result;
	}
}
