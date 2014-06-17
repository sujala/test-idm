package com.rackspace.idm.jython;

import com.rackspace.idm.domain.dao.UserDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class Objects {
    public static ApplicationContext applicationContext;

    @Autowired
    public void setApplicationContext(ApplicationContext applicationContext) {
        Objects.applicationContext = applicationContext;
    }
}
