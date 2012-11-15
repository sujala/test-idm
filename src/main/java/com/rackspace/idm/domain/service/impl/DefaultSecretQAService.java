package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.entity.Question;
import com.rackspace.idm.domain.entity.SecretQA;
import com.rackspace.idm.domain.entity.SecretQAs;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.SecretQAService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.NotFoundException;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 11/13/12
 * Time: 1:03 PM
 * To change this template use File | Settings | File Templates.
 */
@Component
public class DefaultSecretQAService implements SecretQAService{
    @Autowired
    DefaultUserService defaultUserService;

    @Autowired
    DefaultQuestionService defaultQuestionService;

    @Override
    public void addSecretQA(String userId, SecretQA secretQA) {
        User user = defaultUserService.checkAndGetUserById(userId);
        validateSecretQA(secretQA);
        Question question = defaultQuestionService.getQuestion(secretQA.getId());
        if(question == null){
            String errMsg = String.format("Question with Id: %s does not exist.", secretQA.getId());
            throw new NotFoundException(errMsg);
        }
        user.setSecretAnswer(secretQA.getAnswer());
        user.setSecretQuestion(question.getQuestion());
        user.setSecretQuestionId(question.getId());
        defaultUserService.updateUser(user,false);
    }

    private void validateSecretQA(SecretQA secretQA) {
        if(StringUtils.isBlank(secretQA.getId())){
            throw new BadRequestException("Secret question cannot be empty.");
        }
        if(StringUtils.isBlank(secretQA.getAnswer())){
            throw new BadRequestException("Secret answer cannot be empty.");
        }
    }

    @Override
    public SecretQAs getSecretQAs(String userId) {
        SecretQAs secretQAs = new SecretQAs();
        User user =  defaultUserService.checkAndGetUserById(userId);
        SecretQA secretQA = checkAndGetSecretQA(user);
        secretQAs.getSecretqa().add(secretQA);
        return secretQAs;
    }

    private SecretQA checkAndGetSecretQA(User user) {
        SecretQA secretQA = new SecretQA();
        if(StringUtils.isBlank(user.getSecretAnswer()) || StringUtils.isBlank(user.getSecretQuestion())){
            String errMsg = String.format("User with Id: %s does not have a secret question and answer.", user.getId());
            throw new NotFoundException(errMsg);
        }
        if(StringUtils.isBlank(user.getSecretQuestionId())){
            user.setSecretQuestionId("0");
        }
        secretQA.setAnswer(user.getSecretAnswer());
        secretQA.setQuestion(user.getSecretQuestion());
        secretQA.setId(user.getSecretQuestionId());
        return secretQA;
    }
}
