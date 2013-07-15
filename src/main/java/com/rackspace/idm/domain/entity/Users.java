package com.rackspace.idm.domain.entity;

import lombok.Data;

import java.util.List;

@Data
public class Users {
    private int totalRecords;
    private int offset;
    private int limit;
    private List<User> users;
}
