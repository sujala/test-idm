package com.rackspace.idm.validation;

import com.rackspace.idm.exception.BadRequestException;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class DefaultObjectValidator implements ObjectValidator {

    @Autowired
    private ObjectConverter objectConverter;

    private Validator validator;

    @PostConstruct
    public void setup(){
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Override
    public void validate(Object object) {
        List<String> messages = new ArrayList<String>();
        if(objectConverter.isConvertible(object)){
            Object obj = objectConverter.convert(object);
            messages = getViolationMessages(obj);
        }
        if (messages.size() > 0) {
            throw new BadRequestException(StringUtils.join(messages, "\n"));
        }
    }

    private List<String> getViolationMessages(Object obj) {
        List<String> messages = new ArrayList<String>();
        Set<ConstraintViolation<Object>> violations = validator.validate(obj);
        for (ConstraintViolation<Object> violation : violations) {
            messages.add(String.format("%s: %s", violation.getPropertyPath(), violation.getMessage()));
        }

        return messages;
    }
}
