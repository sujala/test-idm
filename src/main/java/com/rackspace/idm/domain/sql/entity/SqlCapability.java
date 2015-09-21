package com.rackspace.idm.domain.sql.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "capability_rax")
public class SqlCapability {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "capability_id", length = 64)
    private String capabilityId;

    @Column(name = "name", length = 64)
    private String name;

    @Column(name = "action", length = 64)
    private String action;

    @Column(name = "url")
    private String url;

    @Column(name = "description")
    private String description;

    @Column(name = "type", length = 64)
    private String type;

    @Column(name = "version")
    private String version;

    /*
     * Foreign key: 'fk_crr_capability_id'
     * Table: capability_resource_rax
     * Key: capability_id
     *
     * Reference table: capability_rax
     * Key: id
     *
     * OnDelete: CASCADE
     */

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "capability_resource_rax",
            joinColumns = @JoinColumn(name = "capability_id"))
    @Column(name = "resource")
    private List<String> resources = new ArrayList<String>();

}
