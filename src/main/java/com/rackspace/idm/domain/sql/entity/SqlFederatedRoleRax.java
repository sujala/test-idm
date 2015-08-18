package com.rackspace.idm.domain.sql.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dozer.Mapping;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "federated_role_rax")
@EqualsAndHashCode(exclude = {"id", "tenantIds"})
public class SqlFederatedRoleRax {

    @Id
    @Column(name = "id", updatable = false)
    private String id;

    @Mapping("id")
    @Column(name = "role_id", updatable = false)
    private String roleRsId;

    @Column(name = "federated_user_rax_id", updatable = false)
    private String userId;

    @Mapping("serviceId")
    @Column(name = "service_id")
    private String clientId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "federated_role_tenant_membership_rax",
            joinColumns = @JoinColumn(name = "federated_role_rax_id"))
    @Column(name = "tenant_id")
    private Set<String> tenantIds = new HashSet<String>();

}



