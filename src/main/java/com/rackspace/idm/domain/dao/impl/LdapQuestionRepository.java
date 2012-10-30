package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.entity.Question;
import com.unboundid.ldap.sdk.SearchResultEntry;
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

    public String[] getSearchAttributes(){
        return ATTR_QUESTION_SEARCH_ATTRIBUTES;
    }

    public String getObjectClass(){
        return OBJECTCLASS_QUESTION;
    }

    public String getUniqueId(Question question){
        return question.getUniqueId();
    }

    public Question getEntry(SearchResultEntry entry) {
        getLogger().debug("Inside getEntryPolicy");
        Question question = new Question();
        question.setId(entry.getAttributeValue(ATTR_ID));
        question.setQuestion(entry.getAttributeValue(ATTR_QUESTION));
        return question;
    }

    public Class getGenericType(){
        return Question.class;
    }
}
