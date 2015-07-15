package com.rackspace.idm.domain.sql.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Entity
@Table(name = "question_rax")
public class SqlQuestion {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "question")
    private String question;
}
