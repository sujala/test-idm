package com.rackspace.idm.audit;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.rackspace.idm.domain.entity.Auditable;
import org.apache.log4j.MDC;
import org.junit.Before;
import org.junit.Test;

import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;


public class AuditTest {

	@Before
	public void before() { 
		MDC.put(Audit.HOST_IP, "127.0.0.1");
		MDC.put(Audit.REMOTE_IP, "10.127.7.164");
		MDC.put(Audit.PATH, "/token");
		MDC.put(Audit.GUUID, UUID.randomUUID().toString());
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
	
	@Test
	public void shouldFailWithMsg() {
		MDC.put(Audit.WHO, "whoId");
		Audit.log("clientId").delete().fail("failMsg");
	}

    @Test
    public void authClient_returnsAudit() throws Exception {
        assertThat("client auth audit",Audit.authClient("string"),instanceOf(Audit.class));
    }

    @Test
    public void authCloudAdmin_returnsAudit() throws Exception {
        assertThat("cloud admin audit",Audit.authCloudAdmin("string"),instanceOf(Audit.class));
    }

    @Test
    public void authRacker_returnsAudit() throws Exception {
        assertThat("racker audit",Audit.authRacker("string"),instanceOf(Audit.class));
    }

    @Test
    public void authClient_withAuditableParameter_returnsAudit() throws Exception {
        Auditable auditable = mock(Auditable.class);
        assertThat("client audit",Audit.authClient(auditable),instanceOf(Audit.class));
    }

    @Test
    public void authRacker_withAuditableParameter_returnsAudit() throws Exception {
        Auditable auditable = mock(Auditable.class);
        assertThat("racker audit",Audit.authRacker(auditable),instanceOf(Audit.class));
    }
}
