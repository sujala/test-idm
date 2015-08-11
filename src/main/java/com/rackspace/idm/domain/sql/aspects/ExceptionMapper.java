package com.rackspace.idm.domain.sql.aspects;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.exception.NotFoundException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;

@Aspect
@SQLComponent
public class ExceptionMapper {

    @Pointcut("execution(void com.rackspace.idm.domain.sql.dao..*.delete*(..))")
    public void delete() {
    }

    @AfterThrowing(pointcut = "delete()", throwing = "e")
    public void afterDelete(JoinPoint joinPoint, Throwable e) {
        if (e instanceof EmptyResultDataAccessException) {
            throw new NotFoundException();
        }
    }

    @Pointcut("execution(* com.rackspace.idm.domain.sql.dao..*.save(..))")
    public void save() {
    }

    @AfterThrowing(pointcut = "save()", throwing = "e")
    public void afterSave(JoinPoint joinPoint, Throwable e) {
        if (e instanceof InvalidDataAccessApiUsageException) {
            throw new IllegalArgumentException();
        }
    }

}
