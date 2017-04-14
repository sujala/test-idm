package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.dao.QuestionDao;
import com.rackspace.idm.domain.entity.Question;
import com.rackspace.idm.domain.sql.dao.QuestionRepository;
import com.rackspace.idm.domain.sql.entity.SqlQuestion;
import com.rackspace.idm.domain.sql.mapper.impl.QuestionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@SQLComponent
public class SqlQuestionRepository implements QuestionDao {

    @Autowired
    private QuestionMapper mapper;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional
    public void addQuestion(Question question) {
        final SqlQuestion sqlQuestion = questionRepository.save(mapper.toSQL(question));

        final Question newQuestion = mapper.fromSQL(sqlQuestion, question);
    }

    @Override
    @Transactional
    public void updateQuestion(Question question) {
        final SqlQuestion sqlQuestion = questionRepository.save(mapper.toSQL(question, questionRepository.findOne(question.getId())));

        final Question newQuestion = mapper.fromSQL(sqlQuestion, question);
    }

    @Override
    @Transactional
    public void deleteQuestion(String questionId) {
        final SqlQuestion sqlQuestion = questionRepository.findOne(questionId);
        questionRepository.delete(questionId);

        final Question newQuestion = mapper.fromSQL(sqlQuestion);
    }

    @Override
    public Question getQuestion(String questionId) {
        return mapper.fromSQL(questionRepository.findOne(questionId));
    }

    @Override
    public Iterable<Question> getQuestions() {
        return mapper.fromSQL(questionRepository.findAll());
    }

    @Override
    public String getNextId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

}
