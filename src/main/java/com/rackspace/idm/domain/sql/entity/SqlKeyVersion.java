package com.rackspace.idm.domain.sql.entity;

import com.rackspace.idm.domain.entity.KeyVersion;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.util.Date;

@Data
@Entity
@Table(name = "keyczar_version_rax")
public class SqlKeyVersion implements KeyVersion {

    @Id
    @Column(name = "id", length = 64)
    private Integer version;

    @Column(name = "created", updatable = false)
    @Temporal(TemporalType.DATE)
    @DateTimeFormat(style = "M-")
    private Date created = new Date();

    @Column(name = "data")
    private String data;

    @Column(name = "metadata_id", length = 64)
    private String metadata;
}
