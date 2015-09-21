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

    /*
     * Foreign key: 'fk_frr_role_id'
     * Table: federated_role_rax
     * Key: role_id
     *
     * Reference table: role
     * Key: id
     *
     * OnDelete: CASCADE
     */

    @Mapping("id")
    @Column(name = "role_id", updatable = false)
    private String roleRsId;


    /*
     * Foreign key: 'fk_frr_federated_user_rax_id'
     * Table: federated_role_rax
     * Key: federated_user_rax_id
     *
     * Reference table: federated_user_rax
     * Key: id
     *
     * OnDelete: CASCADE
     */

    @Column(name = "federated_user_rax_id", updatable = false)
    private String userId;

    /*
     * Foreign key: 'fk_frr_service_id'
     * Table: federated_role_rax
     * Key: service_id
     *
     * Reference table: service
     * Key: id
     *
     * OnDelete: CASCADE
     */

    @Mapping("serviceId")
    @Column(name = "service_id")
    private String clientId;

    /*
     * Foreign key: 'fk_frtmr_federated_role_rax_id'
     * Table: federated_role_tenant_membership_rax
     * Key: federated_role_rax_id
     *
     * Reference table: federated_role_rax
     * Key: id
     *
     * OnDelete: CASCADE
     */

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "federated_role_tenant_membership_rax",
            joinColumns = @JoinColumn(name = "federated_role_rax_id"))
    @Column(name = "tenant_id")
    private Set<String> tenantIds = new HashSet<String>();

}



