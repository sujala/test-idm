package com.rackspace.idm.domain.dao;


import com.rackspace.idm.domain.entity.Question;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 11/12/12
 * Time: 5:24 PM
 * To change this template use File | Settings | File Templates.
 */
public interface QuestionDao {
    void addQuestion(Question question);
    void updateQuestion(Question question);
    void deleteQuestion(String questionId);
    Question getQuestion(String questionId);
    List<Question> getQuestions();
}
