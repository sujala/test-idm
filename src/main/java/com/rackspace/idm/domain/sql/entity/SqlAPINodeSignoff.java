package com.rackspace.idm.domain.sql.entity;

import com.rackspace.idm.domain.dao.APINodeSignoff;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Data
@Entity
@Table(name = "api_node_signoff_rax")
public class SqlAPINodeSignoff implements APINodeSignoff {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @NotNull
    @Column(name = "name", length = 64, unique = true)
    private String nodeName;

    @NotNull
    @Column(name = "key_metadata_id")
    private String keyMetadataId;

    @Column(name = "key_created")
    private Date cachedMetaCreatedDate;

    @Column(name = "loaded_date")
    private Date loadedDate;
}
