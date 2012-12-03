package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.dao.PatternDao;
import com.rackspace.idm.domain.dao.QuestionDao;
import com.rackspace.idm.domain.entity.Pattern;
import com.rackspace.idm.domain.entity.Question;
import com.unboundid.ldap.sdk.Filter;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 10/29/12
 * Time: 1:18 PM
 * To change this template use File | Settings | File Templates.
 */
@Component
public class LdapPatternRepository extends LdapGenericRepository<Pattern> implements PatternDao {

    public String getBaseDn(){
        return QUESTION_BASE_DN;
    }

    public String getLdapEntityClass(){
        return OBJECTCLASS_QUESTION;
    }

    public String getNextId() {
        return getNextId(NEXT_QUESTION_ID);
    }

    @Override
    public List<Pattern> getPatterns(String name) {
        Filter searchFilter = new LdapRepository.LdapSearchBuilder()
                .addEqualAttribute(LdapRepository.ATTR_OBJECT_CLASS, LdapRepository.OBJECTCLASS_PATTERN)
                .addEqualAttribute(LdapRepository.ATTR_NAME, name).build();
        return getObjects(searchFilter);
    }
}
