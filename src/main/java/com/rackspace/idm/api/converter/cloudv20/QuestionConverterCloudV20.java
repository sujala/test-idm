package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.domain.entity.Question;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;
import java.util.List;

@Component
public class QuestionConverterCloudV20 {
    @Autowired
    Mapper mapper;

    @Autowired
    private JAXBObjectFactories objFactories;

    public JAXBElement<com.rackspace.docs.identity.api.ext.rax_auth.v1.Question> toQuestion(Question question) {
        com.rackspace.docs.identity.api.ext.rax_auth.v1.Question questionEntity = mapper.map(
                question, com.rackspace.docs.identity.api.ext.rax_auth.v1.Question.class
        );

        return objFactories.getRackspaceIdentityExtRaxgaV1Factory().createQuestion(questionEntity);
    }

    public Question fromQuestion(com.rackspace.docs.identity.api.ext.rax_auth.v1.Question questionEntity) {
        return mapper.map(questionEntity, Question.class);
    }

    public JAXBElement<com.rackspace.docs.identity.api.ext.rax_auth.v1.Questions> toQuestions(List<Question> questions) {
        com.rackspace.docs.identity.api.ext.rax_auth.v1.Questions questionsEntity = objFactories.getRackspaceIdentityExtRaxgaV1Factory().createQuestions();

        for(Question question : questions) {
            questionsEntity.getQuestion().add(toQuestion(question).getValue());
        }

        return objFactories.getRackspaceIdentityExtRaxgaV1Factory().createQuestions(questionsEntity);
    }
}
