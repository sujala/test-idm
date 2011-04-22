	package com.rackspace.idm.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

import com.rackspace.idm.domain.dao.impl.LdapConnectionPools;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;

@ManagedResource
public class LdapRouterMBean {
	@Autowired
    private LdapConnectionPools connPools;
	
	@ManagedAttribute
	public Integer getNumApplicationAvailableConnections() {
		return connPools.getAppConnPool().getConnectionPoolStatistics().getNumAvailableConnections();
	}
	
	@ManagedAttribute
	public Long getNumApplicationFailedCheckouts() {
		return connPools.getAppConnPool().getConnectionPoolStatistics().getNumFailedCheckouts();
	}
	
	@ManagedAttribute
	public Integer getNumBindAvailableConnections() {
		return connPools.getBindConnPool().getConnectionPoolStatistics().getNumAvailableConnections();
	}
	
	@ManagedAttribute
	public Long getNumBindFailedCheckouts() {
		return connPools.getBindConnPool().getConnectionPoolStatistics().getNumFailedCheckouts();
	}
	
	@ManagedOperation
	public void reset() {
		connPools.getAppConnPool().getConnectionPoolStatistics().reset();
		connPools.getBindConnPool().getConnectionPoolStatistics().reset();
	}
	
	@ManagedAttribute
	public String getBindConfiguration() {
		String params = connPools.getBindConnPool().toString();
		return params;
	}
	
	@ManagedAttribute
	public String getAppConfiguration() {
		String params = connPools.getAppConnPool().toString();
		return params;
	}
	
	@ManagedAttribute
	public Map<String,String> getServerConnectionStatus() {
		String s = connPools.getAppConnPool().toString();
		// Sample: LDAPConnectionPool(name='bind', serverSet=RoundRobinServerSet(servers={server1:389, server2:389}), maxConnections=1000)
		Matcher matcher = Pattern.compile(".*\\{(.*)\\}.*").matcher(s);
		
		Map<String,String> result = new HashMap<String, String>();
		
		boolean found = matcher.find();
		if(found) {
			String serverList = matcher.group(1);
			String[] servers = serverList.split(",");
			for (String server : servers) {
				String[] hostPort = server.split(":");
				try {
					LDAPConnection con;
					con = new LDAPConnection(StringUtils.strip(hostPort[0]), Integer.parseInt(hostPort[1]));
					connPools.getAppConnPool().getHealthCheck().ensureConnectionValidForContinuedUse(con);
					result.put(server, "up");
					con.close();
				} catch (LDAPException e) {
					result.put(server, "down");
				}
			}
		}
		
		return result;
		
	}
}
