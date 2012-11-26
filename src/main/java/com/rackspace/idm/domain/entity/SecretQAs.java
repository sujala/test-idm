package com.rackspace.idm.domain.entity;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 11/13/12
 * Time: 1:38 PM
 * To change this template use File | Settings | File Templates.
 */
public class SecretQAs {
    private List<SecretQA> secretqa;

    public List<SecretQA> getSecretqa() {
        if (secretqa == null) {
            secretqa = new ArrayList<SecretQA>();
        }
        return this.secretqa;
    }
}
