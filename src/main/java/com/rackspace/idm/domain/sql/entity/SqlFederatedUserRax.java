package com.rackspace.idm.domain.sql.entity;

import lombok.Data;
import org.dozer.Mapping;
import org.hibernate.validator.constraints.Length;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Data
@Entity
@Table(name = "federated_user_rax")
public class SqlFederatedUserRax {

    @Id
    @Column(name = "id")
    private String id;

    @NotNull
    @Length(min = 1, max = 100)
    @Column(name = "username")
    private String username;

    @Column(name = "email")
    private String email;

    @Mapping("defaultRegion")
    @Column(name = "region_id")
    private String region;

    @Column(name = "domain_id")
    private String domainId;

    @Column(name = "federated_idp_uri")
    private String federatedIdpUri;

    @Column(name = "created")
    private Date created = new Date();

    @Column(name = "updated")
    private Date updated;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name="federated_user_group_membership_rax",
            joinColumns=@JoinColumn(name="federated_user_rax_id"))
    @Column(name="group_id")
    private Set<String> rsGroupId;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true, mappedBy = "userId")
    private Set<SqlFederatedRoleRax> federatedRoles;

}
