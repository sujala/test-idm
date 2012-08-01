package com.rackspace.idm.audit;

import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.rackspace.idm.domain.entity.Auditable;
import com.unboundid.ldap.sdk.Modification;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Audit {
	public static final String REMOTE_IP = "REMOTE_IP";
	public static final String HOST_IP = "HOST_IP";
	public static final String PATH = "PATH";
	public static final String WHO = "WHO";
	public static final String GUUID = "GUUID";

	private enum ACTION {
		USERAUTH, CLIENTAUTH, RACKERAUTH, ADD, DELETE, MODIFY, CLOUDADMINAUTH
	}

	private enum RESULT {
		SUCCEED, FAIL;
	}

	private final class Event {
		private ACTION action;
		private String context;

		private Event(ACTION action, String context) {
			this.action = action;
			this.context = context;
		}
	}

	private static final String AUDIT_LOGGER_ID = "audit";
	
	private List<Event> events = new ArrayList<Event>();
	private String target;
	private String failureMsg = "-";
	private volatile boolean consumed = false;

	private Audit(String s) {
		target = s;
	}

	public static Audit log(Auditable o) {
		return new Audit(o.getAuditContext());
	}
	
	public static Audit log(String o) {
        return new Audit(o);
    }	

	public static Audit authClient(String o) {
		return new Audit(o).addEvent(ACTION.CLIENTAUTH);
	}
	
	public static Audit authUser(String o) {
		return new Audit(o).addEvent(ACTION.USERAUTH);
	}

    public static Audit authCloudAdmin(String o) {
		return new Audit(o).addEvent(ACTION.CLOUDADMINAUTH);
	}
	
	public static Audit authRacker(String o) {
		return new Audit(o).addEvent(ACTION.RACKERAUTH);
	}
	

	public static Audit authClient(Auditable o) {
        return new Audit(o.getAuditContext()).addEvent(ACTION.CLIENTAUTH);
    }

    public static Audit authUser(Auditable o) {
        return new Audit(o.getAuditContext()).addEvent(ACTION.USERAUTH);
    }
    
    public static Audit authRacker(Auditable o) {
        return new Audit(o.getAuditContext()).addEvent(ACTION.RACKERAUTH);
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
	private static final List<String> secrets;
	static {
		List<String> temp = new ArrayList<String>();
		temp.add(LdapRepository.ATTR_PASSWORD);
		temp.add(LdapRepository.ATTR_CLEAR_PASSWORD);
		temp.add(LdapRepository.ATTR_RACKSPACE_API_KEY);
		secrets = Collections.unmodifiableList(temp);
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
	
	public void fail(String msg) {
		failureMsg = msg;
		fail();
	}

	private void log(RESULT r) {
		if (consumed) {
			LoggerFactory.getLogger(getClass()).error("Audit logger reused");
			return;
		}
		consumed = true;
		Logger logger = LoggerFactory.getLogger(AUDIT_LOGGER_ID);
		for (Event event : events) {
			logger.info(
					r + " {} {} [{}] {} {} {} {} {} {}",
					new Object[] { event.action, target, event.context, StringUtils.defaultIfEmpty(MDC.get(WHO),"-"),
							failureMsg, MDC.get(REMOTE_IP), MDC.get(HOST_IP),
							MDC.get(PATH), MDC.get(GUUID) });
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
