package com.rackspace.idm.domain.entity;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.PolicyAlgorithm;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 9/7/12
 * Time: 3:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class Policies{
    private List<Policy> policy;

    private PolicyAlgorithm algorithm;

    public List<Policy> getPolicy() {
        if (policy == null) {
            policy = new ArrayList<Policy>();
        }
        return this.policy;
    }

    public PolicyAlgorithm getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(PolicyAlgorithm value) {
        this.algorithm = value;
    }
}
