package com.rackspace.idm.aspect;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.DeclareError;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class ArchitecturalEnforcement {
    @Pointcut("within(*..*resource..*)")
    public void withinResource() {}

    @Pointcut("within(*..*service..*)")
    public void withinService() {}

    @Pointcut("within(*..*dao..*)")
    public void withinDao() {}

    @Pointcut("call(* *..*.dao..*(..))")
    public void callDao() {}

    @Pointcut("call(* com.unboundid.ldap.sdk..*(..))")
    public void callLdap() {}

    @Pointcut("call(* *..*LdapSearchBuilder.*(..))")
    public void callLdapSearchBuilder() {}

    @DeclareError("withinResource() && callDao()")
    private static final String RESOURCE_MUST_NOT_USE_DAO = "WebResource must not access dao";

    @DeclareError("withinResource() && callLdap()")
    private static final String RESOURCE_MUST_NOT_USE_LDAP = "WebResource must not access ldap sdk";

    @DeclareError("withinResource() && callLdapSearchBuilder()")
    private static final String RESOURCE_MUST_NOT_USE_LDAP_SEARCH_BUILDER = "WebResource must not access ldap search filter";

    @DeclareError("withinService() && callLdap()")
    private static final String SERVICE_MUST_NOT_USE_LDAP = "Services must not access ldap sdk";

    @DeclareError("withinResource() && callLdapSearchBuilder()")
    private static final String SERVICE_MUST_NOT_USE_LDAP_SEARCH_BUILDER = "Services must not access ldap search filter";

}
