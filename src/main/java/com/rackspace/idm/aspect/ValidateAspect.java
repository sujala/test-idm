package com.rackspace.idm.aspect;

import com.rackspace.idm.validation.ObjectValidator;
import org.apache.commons.configuration.Configuration;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.xml.bind.annotation.XmlRootElement;
import java.lang.annotation.Annotation;

@Aspect
public class ValidateAspect {

    @Autowired
    ObjectValidator objectValidator;

    @Autowired
    Configuration config;

    @Pointcut("execution(* com.rackspace.idm.api.resource..*.*(..))")
    public void resource() {
    }

    @Before("@annotation(post) && resource()")
    public void validateAnnotatedField(JoinPoint joinPoint, POST post) {
        validateObject(joinPoint);
    }

    @Before("@annotation(put) && resource()")
    public void validateAnnotatedField(JoinPoint joinPoint, PUT put) {
        validateObject(joinPoint);
    }

    private void validateObject(JoinPoint joinPoint) {
        if (config != null && !config.getBoolean("validate.entities")) {
            return;
        }

        Object[] args = joinPoint.getArgs();

        for (Object arg : args) {
            if (arg == null) {
                continue;
            }
            for (Annotation annotation : arg.getClass().getAnnotations()) {
                if (annotation.annotationType() == XmlRootElement.class) {
                    if (objectValidator != null) {
                        objectValidator.validate(arg);
                    }
                }
            }
        }
    }
}
