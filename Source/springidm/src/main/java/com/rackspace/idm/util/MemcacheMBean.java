package com.rackspace.idm.util;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.spy.memcached.MemcachedClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

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
}
