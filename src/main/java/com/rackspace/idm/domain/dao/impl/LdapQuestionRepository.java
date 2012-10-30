package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.entity.Question;
import org.springframework.stereotype.Component;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 10/29/12
 * Time: 1:18 PM
 * To change this template use File | Settings | File Templates.
 */
@Component
public class LdapQuestionRepository extends LdapGenericRepository<Question>{

    public String getBaseDn(){
        return QUESTION_BASE_DN;
    }

    public String getLdapEntityClass(){
        return OBJECTCLASS_QUESTION;
    }

    public String[] getSearchAttributes(){
        return ATTR_QUESTION_SEARCH_ATTRIBUTES;
    }
}
