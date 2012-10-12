package com.rackspace.idm.util;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

import org.springframework.stereotype.Component;

@ManagedResource
@Component
public class LoggerMBean {
	private Logger l = Logger.getLogger("com.rackspace.idm");

	@ManagedAttribute
	public String getLogLevel() {
		return l.getLevel().toString();
	}
	
	@ManagedOperation
	public void setLogLevel(String level) {
		l.setLevel(Level.toLevel(level));
		l.info("Logger level for \"com.rackspace.idm\" set to " + l.getLevel());
	}
	
}
