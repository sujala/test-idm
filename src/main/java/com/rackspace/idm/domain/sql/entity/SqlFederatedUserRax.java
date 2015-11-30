package com.rackspace.idm.domain.sql.entity;

import lombok.Data;
import org.dozer.Mapping;
import org.hibernate.validator.constraints.Length;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.Set;

@Data
@Entity
@Table(name = "federated_user_rax")
public class SqlFederatedUserRax {

    /*
     * Foreign key: 'fk_fugmr_federated_user_rax_id'
     * Table: federated_user_group_membership_rax
     * Key: federated_user_rax_id
     *
     * Reference table: federated_user_rax
     * Key: id
     *
     * OnDelete: CASCADE
     */

    @Id
    @Column(name = "id")
    private String id;

    @NotNull
    @Length(min = 1, max = 100)
    @Column(name = "username")
    private String username;

    @Column(name = "email")
    private String email;

    /*
     * Foreign key: 'fk_fur_region_id'
     * Table: federated_user_rax
     * Key: region_id
     *
     * Reference table: region
     * Key: id
     *
     * OnDelete: CASCADE
     */

    @Mapping("defaultRegion")
    @Column(name = "region_id")
    private String region;

    /*
     * Foreign key: 'fk_fur_domain_id'
     * Table: federated_user_rax
     * Key: region_id
     *
     * Reference table: domain
     * Key: id
     *
     * OnDelete: CASCADE
     */

    @Column(name = "domain_id")
    private String domainId;

    @Column(name = "federated_idp_uri")
    private String federatedIdpUri;

    @Column(name = "created")
    private Date created = new Date();

    @Column(name = "updated")
    private Date updated;

    /*
     * Foreign key: 'fk_fugmr_group_id'
     * Table: federated_user_group_membership_rax
     * Key: group_id
     *
     * Reference table: group
     * Key: id
     *
     * OnDelete: CASCADE
     */

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name="federated_user_group_membership_rax",
            joinColumns=@JoinColumn(name="federated_user_rax_id"))
    @Column(name="group_id")
    private Set<String> rsGroupId;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true, mappedBy = "userId")
    private Set<SqlFederatedRoleRax> federatedRoles;

    @Column(name = "expired")
    private Date expiredTimestamp;

}
