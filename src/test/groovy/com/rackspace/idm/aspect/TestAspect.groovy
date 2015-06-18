package com.rackspace.idm.aspect

import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.annotation.Pointcut
import org.springframework.stereotype.Component


@Aspect
@Component
public class TestAspect {

    public static Integer INTERCEPT_VALUE = 1

    @Pointcut("execution(* com.rackspace.idm.aspect.TestAspectTarget.*(..))")
    public void fooCalledWithAop() {

    }

    @Before("fooCalledWithAop()")
    public void validateAnnotatedField(JoinPoint joinPoint) {
        if(joinPoint.getArgs()[0].equals(INTERCEPT_VALUE)) {
            throw new RuntimeException("Aspect throwing exception")
        }
    }

}
