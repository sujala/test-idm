package com.rackspace.idm.domain.sql.entity;


import lombok.Data;
import lombok.ToString;

import javax.persistence.*;

@Data
@Entity
@ToString(exclude = "tokenRevocationRecord")
@Table(name = "revocation_event_token_rax")
public class SqlTokenRevocationRecordTokenRax {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @OneToOne(optional = false)
    @JoinColumn(name = "revocation_event_id", nullable = false, updatable = false)
    private SqlTokenRevocationRecord tokenRevocationRecord;

    @Column(name = "access_token", nullable = false, updatable = false)
    private String token;


}
