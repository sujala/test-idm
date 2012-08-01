	package com.rackspace.idm.util;

    import com.rackspace.idm.domain.dao.impl.LdapConnectionPools;
    import com.unboundid.ldap.sdk.LDAPConnection;
    import com.unboundid.ldap.sdk.LDAPException;
    import org.apache.commons.lang.StringUtils;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.jmx.export.annotation.ManagedAttribute;
    import org.springframework.jmx.export.annotation.ManagedOperation;
    import org.springframework.jmx.export.annotation.ManagedResource;

    import java.util.HashMap;
    import java.util.Map;
    import java.util.regex.Matcher;
    import java.util.regex.Pattern;

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
		return connPools.getBindConnPool().toString();
	}
	
	@ManagedAttribute
	public String getAppConfiguration() {
		return connPools.getAppConnPool().toString();
	}
	
	@ManagedAttribute
	public Map<String,String> getServerConnectionStatus() {
		String s = connPools.getAppConnPool().toString();
		// Sample: LDAPConnectionPool(name='bind', serverSet=RoundRobinServerSet(servers={server1:389, server2:389}), maxConnections=1000)
		Matcher matcher = Pattern.compile("((?:[\\w-]+\\.)*[\\w-]*)\\:(\\d+)").matcher(s); //Group 1 is server, group 2 is port.
		
		Map<String,String> result = new HashMap<String, String>();
		
        while(matcher.find()){
            try {
                LDAPConnection con;
                con = new LDAPConnection(StringUtils.strip(matcher.group(1)), Integer.parseInt(matcher.group(2)));
                connPools.getAppConnPool().getHealthCheck().ensureConnectionValidForContinuedUse(con);
                result.put(matcher.group(1), "up");
                con.close();
            } catch (LDAPException e) {
                result.put(matcher.group(1), "down");
            }
        }

		return result;
		
	}

    public void setConnPools(LdapConnectionPools connPools) {
        this.connPools = connPools;
    }
}
