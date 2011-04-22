package com.rackspace.idm.util;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
     public Map<String,String> getServerConnectionStatus() {
          
            Map<String,String> result = new HashMap<String, String>();
            
            Collection<SocketAddress> servers = memcached.getAvailableServers();
            servers.addAll(memcached.getUnavailableServers());
            
            Map<SocketAddress,Map<String,String>> stats = memcached.getStats();
            for(SocketAddress server : servers) {

                String serverStr = server.toString();
                serverStr = serverStr.replaceAll("/", "");

                if(stats.get(server).isEmpty()) {
                    result.put(serverStr, "down");
                } else {
                    result.put(serverStr, "up");
                }
            }
            
            return result;
        }    
}
