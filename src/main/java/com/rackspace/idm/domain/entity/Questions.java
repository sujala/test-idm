package com.rackspace.idm.domain.entity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 10/26/12
 * Time: 3:30 PM
 * To change this template use File | Settings | File Templates.
 */

public class Questions {
    private List<Question> question;

    public List<Question> getQuestion() {
        if (question == null) {
            question = new ArrayList<Question>();
        }
        return this.question;
    }
}
