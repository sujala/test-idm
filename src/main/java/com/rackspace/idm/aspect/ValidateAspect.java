package com.rackspace.idm.aspect;

import com.rackspace.idm.annotation.ValidateParam;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import javax.ws.rs.POST;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

@Component
@Aspect
public class ValidateAspect {

    /*
    @Around("@annotation(validate)")
    public Object processAttribute(ProceedingJoinPoint pjp, Allow validate) throws Throwable {
        Object result = pjp.proceed();
        return result;
    }
    */

    @Before("@annotation(post) && execution(* com.rackspace.idm.api.resource.cloud.v20..*.*(..))")
    public void validateAnnotatedField(JoinPoint joinPoint, POST post) {
        Object[] args = joinPoint.getArgs();
        MethodSignature ms = (MethodSignature) joinPoint.getSignature();
        Method m = ms.getMethod();

        Annotation[][] parameterAnnotations = m.getParameterAnnotations();

        for (int i = 0; i < parameterAnnotations.length; i++) {
            Annotation[] annotations = parameterAnnotations[i];

            for (Annotation annotation : annotations) {
                if (annotation.annotationType() == ValidateParam.class) {
                    Object param = args[i];
                    //call validate on param
                }
            }
        }
    }
}
