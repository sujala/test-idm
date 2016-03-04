package com.rackspace.idm.audit;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.ForgotPasswordCredentials;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.validation.Validator20;
import com.unboundid.ldap.sdk.Modification;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class Audit {
	public static final String REMOTE_IP = "REMOTE_IP";
	public static final String HOST_IP = "HOST_IP";
	public static final String PATH = "PATH";
	public static final String WHO = "WHO";
	public static final String GUUID = "GUUID";
    public static final String X_FORWARDED_FOR = "X_FORWARDED_FOR";

	private enum ACTION {
		USERAUTH,
		CLIENTAUTH,
		RACKERAUTH,
		ADD,
		DELETE,
		MODIFY,
		CLOUDADMINAUTH,
		IMPERSONATION,
		FEDERATEDAUTH,
		FORGOTPWDAUTH,
		PASSWORD_RESET,
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

	public static Audit authFederated(FederatedBaseUser o) {
		return new Audit(o.getAuditContext()).addEvent(ACTION.FEDERATEDAUTH);
	}

	public static Audit authForgotUser(User o) {
		return new Audit(o.getAuditContext()).addEvent(ACTION.FORGOTPWDAUTH, String.format("userId='%s', email='%s'", o.getId(), o.getEmail()));
	}

	public static Audit authImpersonation(Auditable o) {
		return new Audit(o.getAuditContext()).addEvent(ACTION.IMPERSONATION);
	}

	public static void logSuccessfulFederatedAuth(FederatedBaseUser federatedBaseUser) {
		Audit audit = authFederated(federatedBaseUser);
		audit.succeed();
	}

	public static void logSuccessfulImpersonation(ImpersonatedScopeAccess scopeAccess) {
		Audit audit = authImpersonation(scopeAccess);
		audit.succeed();
	}

	public static void logSuccessfulForgotPasswordRequest(ForgotPasswordCredentials forgotPasswordCredentials, User user) {
		Audit audit = new Audit(user.getAuditContext()).addEvent(ACTION.FORGOTPWDAUTH, String.format("userId='%s', email='%s', portal='%s'", user.getId(), user.getEmail(),  StringUtils.left(forgotPasswordCredentials.getPortal(), 100)));
		audit.succeed();
	}

	public static void logFailedForgotPasswordRequest(ForgotPasswordCredentials forgotPasswordCredentials, String failureReason) {
		//defensive programming around user supplied value which could be excessively long. Don't want to risk filling
		//up logs
		String finalPortal = StringUtils.left(forgotPasswordCredentials.getPortal(), Validator20.MAX_USERNAME + 10);
		String finalUsername = StringUtils.left(forgotPasswordCredentials.getUsername(), Validator20.MAX_USERNAME + 10);
		StringBuilder buf = new StringBuilder();
		buf.append(String.format("failureReason=%s, portal='%s'", failureReason, finalPortal));

		if (finalPortal != null && finalPortal.length() != forgotPasswordCredentials.getPortal().length()) {
			buf.append(String.format(", portalTruncated from '%d' length", forgotPasswordCredentials.getPortal().length()));
		}
		if (finalUsername != null && finalUsername.length() != forgotPasswordCredentials.getUsername().length()) {
			buf.append(String.format(", usernameTruncated from '%d' length", forgotPasswordCredentials.getUsername().length()));
		}

		String context = buf.toString();

		Audit audit = new Audit(String.format("username=%s", finalUsername)).addEvent(ACTION.FORGOTPWDAUTH, context);
		audit.fail();
	}

	public static void logSuccessfulPasswordResetRequest(User user) {
		Audit audit = new Audit(user.getAuditContext()).addEvent(ACTION.PASSWORD_RESET, String.format("userId='%s', pwdResetTokenUsed=%s", user.getId(), Boolean.TRUE));
		audit.succeed();
	}

	public static Audit deleteOTP(String container) {
        return new Audit(container).addEvent(ACTION.DELETE);
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
	private static final List<String> SECRETS;
	static {
		List<String> temp = new ArrayList<String>();
		temp.add(LdapRepository.ATTR_PASSWORD);
		temp.add(LdapRepository.ATTR_CLEAR_PASSWORD);
		temp.add(LdapRepository.ATTR_RACKSPACE_API_KEY);
		SECRETS = Collections.unmodifiableList(temp);
	}

	public Audit modify(List<Modification> mods) {
		// obfuscate our secret attributes
		for (Modification mod : mods) {
			if (SECRETS.contains(mod.getAttributeName())) {
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
					r + " {} {} [{}] {} {} Remote: {} Host: {} X-Forwarded-For: {} {} {}",
					new Object[] { event.action, target, event.context, StringUtils.defaultIfEmpty(MDC.get(WHO),"-"),
							failureMsg, MDC.get(REMOTE_IP),
                            MDC.get(HOST_IP),
                            MDC.get(X_FORWARDED_FOR),
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
