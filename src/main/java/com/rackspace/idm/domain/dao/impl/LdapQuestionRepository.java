package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.LDAPComponent;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.QuestionDao;
import com.rackspace.idm.domain.entity.Question;
import com.unboundid.ldap.sdk.Filter;
import org.springframework.beans.factory.annotation.Autowired;

@LDAPComponent
public class LdapQuestionRepository extends LdapGenericRepository<Question> implements QuestionDao {

    @Autowired
    private IdentityConfig identityConfig;

    public String getBaseDn(){
        return QUESTION_BASE_DN;
    }

    public String getLdapEntityClass(){
        return OBJECTCLASS_QUESTION;
    }

    public String getNextId() {
        return getUuid();
    }

    public String getSortAttribute() {
        return ATTR_ID;
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
    public Iterable<Question> getQuestions() {
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
