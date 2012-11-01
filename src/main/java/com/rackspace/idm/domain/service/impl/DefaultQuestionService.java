package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.impl.LdapQuestionRepository;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.rackspace.idm.domain.entity.Question;
import com.rackspace.idm.domain.service.QuestionService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;
import com.unboundid.ldap.sdk.Filter;
import org.apache.cxf.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DefaultQuestionService implements QuestionService {

    @Autowired
    LdapQuestionRepository questionDao;

    @Override
    public void addQuestion(Question question) {
        validateQuestion(question);
        Question duplicateQuestion = getQuestionSearchFilter(question.getId());

        if (duplicateQuestion != null) {
            throw new DuplicateException("question with id already exists");
        }

        questionDao.addObject(question);
    }

    @Override
    public void updateQuestion(String questionId, Question question) {
        validateQuestionId(questionId);
        validateQuestion(question);

        if (!questionId.equals(question.getId())) {
            throw new BadRequestException("question id does not match");
        }

        Question oldQuestion = checkAndGetQuestion(questionId);
        question.setLdapEntry(oldQuestion.getLdapEntry());

        questionDao.updateObject(question);
    }

    @Override
    public void deleteQuestion(String questionId) {
        validateQuestionId(questionId);
        Question question = checkAndGetQuestion(questionId);

        questionDao.deleteObject(getSearchFilter(questionId));
    }

    @Override
    public Question getQuestion(String questionId) {
        validateQuestionId(questionId);
        Question oldQuestion = checkAndGetQuestion(questionId);
        return oldQuestion;
    }

    @Override
    public List<Question> getQuestions() {
        Filter searchFilter = new LdapRepository.LdapSearchBuilder().addEqualAttribute(LdapRepository.ATTR_OBJECT_CLASS, LdapRepository.OBJECTCLASS_QUESTION).build();
        return questionDao.getObjects(searchFilter);
    }

    private void validateQuestion(Question question) {
        if (question == null) {
            throw new BadRequestException("question cannot be null");
        }

        validateQuestionId(question.getId());

        if (StringUtils.isEmpty(question.getQuestion())) {
            throw new BadRequestException("missing required question");
        }
    }

    private Question checkAndGetQuestion(String questionId) {
        Question question = getQuestionSearchFilter(questionId);
        if (question == null) {
            throw new NotFoundException(String.format("Question width id %s does not exist", questionId));
        }
        return question;
    }

    private Question getQuestionSearchFilter(String questionId) {
        return questionDao.getObject(getSearchFilter(questionId));
    }

    private Filter getSearchFilter(String questionId) {
        return new LdapRepository.LdapSearchBuilder()
                    .addEqualAttribute(LdapRepository.ATTR_ID, questionId).build();
    }

    private void validateQuestionId(String questionId) {
        if (StringUtils.isEmpty(questionId)) {
            throw new BadRequestException("missing required question id");
        }
    }
}
