package com.rackspace.idm.audit;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 7/20/12
 * Time: 12:45 PM
 * To change this template use File | Settings | File Templates.
 */
public class TraceLoggerTest {

    TraceLogger traceLogger;

    @Before
    public void setUp() throws Exception {
        traceLogger = new TraceLogger();
    }

    @Test
    public void trace_succeeds() throws Exception {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature sig = mock(Signature.class);
        Object success = false;
        try{
            when(joinPoint.getSignature()).thenReturn(sig);
            when(sig.getDeclaringTypeName()).thenReturn("string");
            when(joinPoint.proceed()).thenReturn(true);
            success  = traceLogger.trace(joinPoint);
        } catch (Throwable throwable){

        }
        assertThat("program ran successfully",success,equalTo((Object) true));
    }
}
