package com.rackspace.idm.domain.sql.aspects;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.core.Ordered;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;

@Aspect
@SQLComponent
public class ExceptionMapper implements Ordered {

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
            throw new IllegalArgumentException(e);
        }
    }

    @Pointcut("execution(* com.rackspace.idm.domain.dao.impl..Sql*.add*(..))")
    public void add() {
    }

    @AfterThrowing(pointcut = "add()", throwing = "e")
    public void afterAdd(JoinPoint joinPoint, Throwable e) {
        if (e instanceof ConstraintViolationException || e instanceof DataIntegrityViolationException) {
            throw new DuplicateException();
        }
    }

    @Override
    public int getOrder() {
        return 1;
    }

}
