package com.rackspace.idm.domain.entity;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Users {
    private int totalRecords;
    private int offset;
    private int limit;
    private List<User> users;

    public List<User> getUsers() {
        if (users == null) {
            users = new ArrayList<User>();
        }
        return users;
    }
}
