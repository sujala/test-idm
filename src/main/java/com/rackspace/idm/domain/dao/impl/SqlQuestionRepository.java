package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.dao.QuestionDao;
import com.rackspace.idm.domain.entity.Question;
import com.rackspace.idm.domain.sql.dao.QuestionRepository;
import com.rackspace.idm.domain.sql.mapper.impl.QuestionMapper;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

@SQLComponent
public class SqlQuestionRepository implements QuestionDao {

    @Autowired
    QuestionMapper mapper;

    @Autowired
    QuestionRepository questionRepository;


    @Override
    public void addQuestion(Question question) {
        questionRepository.save(mapper.toSQL(question));
    }

    @Override
    public void updateQuestion(Question question) {
        questionRepository.save(mapper.toSQL(question));
    }

    @Override
    public void deleteQuestion(String questionId) {
        questionRepository.delete(questionId);
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
