package com.rackspace.idm.domain.migration.ldap.entity;

import com.rackspace.idm.domain.migration.ChangeType;
import lombok.Data;

import javax.persistence.*;
import java.util.Date;

@Data
@Entity
@Table(name = "delta_ldap_to_sql_rax")
public class LdapToSqlEntity {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event", length = 15)
    private ChangeType event;

    @Column(name = "host", length = 255)
    private String host;

    @Column(name = "type", length = 255)
    private String type;

    @Column(name = "data")
    private String data = "";

    @Column(name = "error")
    private String error;

    @Column(name = "created", updatable = false, insertable = false)
    private Date created;

    @Column(name = "migrated")
    private Date migrated;

    @Column(name = "retrieved")
    private Date retrieved;

}
