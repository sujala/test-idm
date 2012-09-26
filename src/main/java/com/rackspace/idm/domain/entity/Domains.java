package com.rackspace.idm.domain.entity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 9/25/12
 * Time: 6:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class Domains {

    protected List<Domain> domain;

    public List<Domain> getDomain() {
        if (domain == null) {
            domain = new ArrayList<Domain>();
        }
        return this.domain;
    }
}
