package com.rackspace.idm.audit;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.MDC;
import org.junit.Before;
import org.junit.Test;

import com.rackspace.idm.dao.LdapRepository;
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
		Audit.log("clientId").delete().succeed();
	}
	
	@Test
	public void shouldLogUserAuth() {
		Audit.authUser("userID").succeed();
	}
	
	@Test
	public void shouldObfuscatePassword() {
		List<Modification> mods = new ArrayList<Modification>();
		Modification mod = new Modification(ModificationType.REPLACE, LdapRepository.ATTR_PASSWORD, "secret");
		mods.add(mod);
		mod = new Modification(ModificationType.REPLACE, LdapRepository.ATTR_RACKSPACE_API_KEY, "secret");
		mods.add(mod);
		Audit.log("clientId").modify(mods).fail();
	}
	
}
