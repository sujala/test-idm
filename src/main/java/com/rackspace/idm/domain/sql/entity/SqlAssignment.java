
package com.rackspace.idm.domain.sql.entity;

import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Data
@MappedSuperclass
@Table(name = "assignment")
@IdClass(value = SqlAssignmentId.class)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
public abstract class SqlAssignment {

    @Id
    @NotNull
    @Column(name = "actor_id", length = 64)
    private String actorId;

    @Id
    @NotNull
    @Column(name = "target_id", length = 64)
    private String targetId;

    @Id
    @NotNull
    @Column(name = "role_id", length = 64)
    private String roleId;

    @Column(name = "inherited", nullable = false)
    private boolean inherited;
}