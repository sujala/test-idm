package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.dao.QuestionDao;
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
public class LdapQuestionRepository extends LdapGenericRepository<Question> implements QuestionDao{

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
    public void addQuestion(Question question) {
        addObject(question);
    }

    @Override
    public void updateQuestion(Question question) {
        updateObject(question);
    }

    @Override
    public void deleteQuestion(String questionId) {
        deleteObject(getSearchFilter(questionId));
    }

    @Override
    public Question getQuestion(String questionId) {
        return getQuestionSearchFilter(questionId);
    }

    @Override
    public List<Question> getQuestions() {
        Filter searchFilter = new LdapRepository.LdapSearchBuilder().addEqualAttribute(LdapRepository.ATTR_OBJECT_CLASS, LdapRepository.OBJECTCLASS_QUESTION).build();
        return getObjects(searchFilter);
    }

    private Question getQuestionSearchFilter(String questionId) {
        return getObject(getSearchFilter(questionId));
    }

    private Filter getSearchFilter(String questionId) {
        return new LdapRepository.LdapSearchBuilder()
                    .addEqualAttribute(LdapRepository.ATTR_ID, questionId).build();
    }
}
