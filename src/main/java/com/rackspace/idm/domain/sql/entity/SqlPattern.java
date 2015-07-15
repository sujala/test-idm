package com.rackspace.idm.domain.sql.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Entity
@Table(name = "pattern_rax")
public class SqlPattern {

    @Id
    @Column(name = "id", length = 64)
    private String name;

    @Column(name = "regex")
    private String regex;

    @Column(name = "error_message")
    private String errMsg;

    @Column(name = "description")
    private String description;

}
