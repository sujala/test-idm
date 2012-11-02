package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.Question;

import java.util.List;

public interface QuestionService {
    String addQuestion(Question question);
    void updateQuestion(String questionId, Question question);
    void deleteQuestion(String questionId);
    Question getQuestion(String questionId);
    List<Question> getQuestions();
}
