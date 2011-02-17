package com.rackspace.idm.audit;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.rackspace.idm.dao.LdapRepository;
import com.unboundid.ldap.sdk.Modification;

public class Audit {
	public static final String REMOTE_IP = "REMOTE_IP";
	public static final String HOST_IP = "HOST_IP";
	public static final String PATH = "PATH";

	private enum ACTION {
		USERAUTH, CLIENTAUTH, RACKERAUTH, ADD, DELETE, MODIFY;
	}

	private enum RESULT {
		SUCCEED, FAIL;
	}

	private class Event {
		private ACTION action;
		private String context;

		Event(ACTION action, String context) {
			this.action = action;
			this.context = context;
		}
	}

	private static final String AUDIT_LOGGER_ID = "audit";
	
	private List<Event> events = new ArrayList<Event>();
	private String target;
	private final long timestamp = System.currentTimeMillis();
	private volatile boolean consumed = false;

	private Audit(String s) {
		target = s;
	}

	public static Audit log(Object o) {
		return new Audit(o.toString());
	}

	public static Audit authClient(Object o) {
		return new Audit(o.toString()).addEvent(ACTION.CLIENTAUTH);
	}

	public static Audit authUser(Object o) {
		return new Audit(o.toString()).addEvent(ACTION.USERAUTH);
	}
	
	public static Audit authRacker(Object o) {
		return new Audit(o.toString()).addEvent(ACTION.RACKERAUTH);
	}

	public Audit add() {
		return this.addEvent(ACTION.ADD);
	}

	public Audit delete() {
		return this.addEvent(ACTION.DELETE);
	}

	public Audit modify() {
		return this.addEvent(ACTION.MODIFY);
	}

	// these attributes will be obfuscated
	private static final List<String> secrets = new ArrayList<String>();
	{
		secrets.add(LdapRepository.ATTR_PASSWORD);
		secrets.add(LdapRepository.ATTR_RACKSPACE_API_KEY);
	}

	public Audit modify(List<Modification> mods) {
		// obfuscate our secret attributes
		for (Modification mod : mods) {
			if (secrets.contains(mod.getAttributeName())) {
				mod = new Modification(mod.getModificationType(),
						mod.getAttributeName(), "*****");
			}
			addEvent(ACTION.MODIFY, mod.toString());
		}
		return this;
	}

	public void succeed() {
		log(RESULT.SUCCEED);
	}

	public void fail() {
		log(RESULT.FAIL);
	}

	private void log(RESULT r) {
		if (consumed) {
			LoggerFactory.getLogger(getClass()).error("Audit logger reused");
		}
		consumed = true;
		Logger logger = LoggerFactory.getLogger(AUDIT_LOGGER_ID);
		for (Event event : events) {
			logger.info(
					r + " {} {} [{}] {} {} {} {}",
					new Object[] { event.action, target, event.context,
							MDC.get(REMOTE_IP), MDC.get(HOST_IP),
							MDC.get(PATH), timestamp });
		}
	}

	private Audit addEvent(ACTION a) {
		return addEvent(a, "-");
	}

	private Audit addEvent(ACTION a, String context) {
		events.add(new Event(a, context));
		return this;
	}
}
