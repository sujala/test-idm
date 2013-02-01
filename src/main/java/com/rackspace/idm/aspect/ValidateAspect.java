package com.rackspace.idm.aspect;

import com.rackspace.idm.annotation.ValidateParam;
import com.rackspace.idm.exception.BadRequestException;
import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.ws.rs.POST;
import javax.xml.bind.annotation.XmlRootElement;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

        for (Object arg : args) {
            for (Annotation annotation : arg.getClass().getAnnotations()) {
                if (annotation.annotationType() == XmlRootElement.class) {
                    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
                    Validator validator = factory.getValidator();
                    Set<ConstraintViolation<Object>> violations = validator.validate(arg);
                    List<String> messages = new ArrayList<String>();
                    for (ConstraintViolation<Object> violation : violations) {
                        messages.add(String.format("%s: %s", violation.getPropertyPath(), violation.getMessage()));
                    }
                    if (violations.size() > 0) {
                        throw new BadRequestException(StringUtils.join(messages, "\n"));
                    }
                }
            }
        }
    }
}
