package com.rackspace.idm.audit;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.MDC;
import org.junit.Before;
import org.junit.Test;

import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;


public class AuditTest {

	@Before
	public void before() {
		MDC.put(Audit.HOST_IP, "127.0.0.1");
		MDC.put(Audit.REMOTE_IP, "10.127.7.164");
		MDC.put(Audit.PATH, "/token");
	}
	@Test
	public void shouldLogEvent() {
		Audit.log("clientId").modify().succeed();
	}
	
	@Test
	public void shouldLogUserAuth() {
		Audit.authUser("userID").succeed();
	}
	
	@Test
	public void shouldLogModifications() {
		MDC.put(Audit.PATH, "/modify");
		List<Modification> mods = new ArrayList<Modification>();
		mods.add(new Modification(ModificationType.REPLACE, "firstname"));
		mods.add(new Modification(ModificationType.ADD, "email"));
		Audit.log("userId").modify(mods).succeed();
	}
}
