package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.impl.LdapQuestionRepository;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.rackspace.idm.domain.entity.Question;
import com.rackspace.idm.domain.service.QuestionService;
import com.rackspace.idm.exception.BadRequestException;
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
    public String addQuestion(Question question) {
        validateQuestion(question);
        question.setId(questionDao.getNextId());
        questionDao.addQuestion(question);
        return question.getId();
    }

    @Override
    public void updateQuestion(String questionId, Question question) {
        validateQuestionId(questionId);
        validateQuestion(question);

        if (question.getId() != null && !questionId.equals(question.getId())) {
            throw new BadRequestException("question id does not match");
        }

        Question oldQuestion = checkAndGetQuestion(questionId);
        question.setLdapEntry(oldQuestion.getLdapEntry());

        questionDao.updateQuestion(question);
    }

    @Override
    public void deleteQuestion(String questionId) {
        validateQuestionId(questionId);
        checkAndGetQuestion(questionId);

        questionDao.deleteQuestion(questionId);
    }

    @Override
    public Question getQuestion(String questionId) {
        validateQuestionId(questionId);
        Question oldQuestion = checkAndGetQuestion(questionId);
        return oldQuestion;
    }

    @Override
    public List<Question> getQuestions() {
        return questionDao.getQuestions();
    }

    private void validateQuestion(Question question) {
        if (question == null) {
            throw new BadRequestException("question cannot be null");
        }

        if (StringUtils.isEmpty(question.getQuestion())) {
            throw new BadRequestException("missing required question");
        }
    }

    private Question checkAndGetQuestion(String questionId) {
        Question question = questionDao.getQuestion(questionId);
        if (question == null) {
            throw new NotFoundException(String.format("Question with id %s does not exist", questionId));
        }
        return question;
    }

    private void validateQuestionId(String questionId) {
        if (StringUtils.isEmpty(questionId)) {
            throw new BadRequestException("missing required question id");
        }
    }
}
