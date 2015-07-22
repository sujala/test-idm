package com.rackspace.idm.domain.sql.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "revocation_event_authenticated_by_rax")
public class SqlTokenRevocationRecordAuthenticatedByRax {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "revocation_event_id", updatable = false, nullable = false)
    private SqlTokenRevocationRecord tokenRevocationRecord;

    @Column(name = "authenticated_by", nullable = false, updatable = false)
    private String authenticatedBy;

}
