package com.rackspace.idm.domain.migration.ldap.entity;

import com.rackspace.idm.domain.migration.ChangeType;
import lombok.Data;

import javax.persistence.*;
import java.util.Date;

@Data
public class LdapToSqlEntity {
    private String id;
    private ChangeType event;
    private String host;
    private String type;
    private String data = "";
    private String error;
    private Date created;
    private Date migrated;
    private Date retrieved;
}
