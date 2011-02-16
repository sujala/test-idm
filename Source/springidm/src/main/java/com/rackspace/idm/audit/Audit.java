package com.rackspace.idm.audit;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.unboundid.ldap.sdk.Modification;

public class Audit {
	public static final String REMOTE_IP = "REMOTE_IP";
	public static final String HOST_IP = "HOST_IP";
	public static final String PATH = "PATH";

	private enum ACTION {
		USERAUTH, CLIENTAUTH, ADD, DELETE, MODIFY;
	}

	private enum RESULT {
		SUCCEED, FAIL;
	}

	private class Event {
		private ACTION action;
		private String target;

		Event(ACTION action, String target) {
			this.action = action;
			this.target = target;
		}
	}

	private static final String AUDIT_LOGGER_ID = "audit";
	
	private List<Event> events = new ArrayList<Event>();
	private String source;
	private long timestamp;

	private Audit(String s) {
		source = s;
		timestamp = System.currentTimeMillis();
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

	public Audit add() {
		return this.addEvent(ACTION.ADD);
	}

	public Audit delete() {
		return this.addEvent(ACTION.DELETE);
	}

	public Audit modify() {
		return this.addEvent(ACTION.MODIFY);
	}

	public Audit modify(List<Modification> mods) {
		for (Modification mod : mods) {
			addEvent(ACTION.MODIFY, mod.getModificationType().getName().toLowerCase() + " " + mod.getAttributeName());
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
		Logger logger = LoggerFactory.getLogger(AUDIT_LOGGER_ID);
		for (Event event : events) {
			logger.info(
					r + " {} {} [{}] {} {} {} {}",
					new Object[] { event.action, source, event.target,
							MDC.get(REMOTE_IP), MDC.get(HOST_IP),
							MDC.get(PATH), timestamp });
		}
	}

	private Audit addEvent(ACTION a) {
		return addEvent(a, "-");
	}

	private Audit addEvent(ACTION a, String target) {
		events.add(new Event(a, target));
		return this;
	}
}
