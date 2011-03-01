package com.rackspace.idm.audit;

import org.apache.commons.lang.time.StopWatch;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.LoggerFactory;

@Aspect
public class TraceLogger {

	@Around("execution(* com.rackspace.idm..*.*(..))")
	public Object trcae(ProceedingJoinPoint joinPoint) throws Throwable {
		Signature sig = joinPoint.getSignature();
		StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        LoggerFactory.getLogger(sig.getDeclaringTypeName()).trace(joinPoint + " entered");

        Object retVal = joinPoint.proceed();

		LoggerFactory.getLogger(sig.getDeclaringTypeName()).trace(joinPoint + " : exit " + stopWatch.getTime() + " ms");
		return retVal;
	}
}
