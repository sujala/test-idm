package com.rackspace.idm.api.security

import com.rackspace.idm.audit.Audit
import com.rackspace.idm.domain.entity.BaseUserToken
import com.rackspace.idm.domain.entity.ScopeAccess
import org.slf4j.MDC
import spock.lang.Specification

class SecurityContextTest extends Specification {

    def "Caller is set in MDC when token set"() {
        SecurityContext ctx = new SecurityContext()
        ScopeAccess callerToken = Mock()
        ScopeAccess effectiveCallerToken = Mock()
        def auditctx = "context info"

        when:
        ctx.setCallerTokens(callerToken, effectiveCallerToken)

        then:
        1 * callerToken.getAuditContext() >> auditctx
        MDC.get(Audit.WHO) == auditctx
    }

    def "Caller is not set in MDC when token is null"() {
        SecurityContext ctx = new SecurityContext()

        when: "token is set to null"
        ctx.setCallerTokens(null, null)

        then: "mdc caller not set"
        MDC.get(Audit.WHO) == null

        when: "If mdc already has a value when token set to null"
        MDC.put(Audit.WHO, "avalue")
        ctx.setCallerTokens(null, null)

        then: "mdc caller is removed"
        MDC.get(Audit.WHO) == null
    }

}
