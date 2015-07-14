package com.rackspace.idm.domain.sql.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.List;
import java.util.Set;

@Data
@Entity
@Table(name = "endpoint_rax")
public class SqlEndpointRax {

    @Id
    @Column(name = "id", length = 255)
    private String id;

    @Column(name = "type", length = 255)
    private String baseUrlType;

    @Column(name = "openstack_type")
    private String openstackType;

    @Column(name = "`default`")
    private boolean def;

    @Column(name = "global")
    private boolean global;

    @Column(name = "service_name")
    private String serviceName;

    /*
     * NOTE: tenantAlias will always be populated to its default value. "{tenant}"
     */
    @Column(name = "project_alias")
    private String tenantAlias;

    @Column(name =  "version_id")
    private String versionId;

    @Column(name =  "version_info")
    private String versionInfo;

    @Column(name =  "version_list")
    private String versionList;

    @OneToMany(cascade = CascadeType.DETACH)
    @JoinColumn(name = "endpoint_id", nullable = true, insertable = false, updatable = false)
    private Set<SqlPolicyEndpointRax> sqlPolicyEndpointRax;
}
