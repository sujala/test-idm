package com.rackspace.idm.audit;

import org.apache.commons.lang.time.StopWatch;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.LoggerFactory;

@Aspect
public class TraceLogger {

	@Around("execution(* com.rackspace.idm.api..*.*(..)) " +
			"|| execution(* com.rackspace.idm.domain.dao.impl..*.*(..))" +
			"|| execution(* com.rackspace.idm.domain.service.impl..*.*(..))")
	public Object trace(ProceedingJoinPoint joinPoint) throws Throwable {
		Signature sig = joinPoint.getSignature();
		StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        LoggerFactory.getLogger(sig.getDeclaringTypeName()).trace("{} : enter", joinPoint);

        Object retVal = joinPoint.proceed();

		LoggerFactory.getLogger(sig.getDeclaringTypeName()).trace("{} : exit {} ms", joinPoint , stopWatch.getTime());
		return retVal;
	}
}
