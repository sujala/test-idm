package com.rackspace.idm.domain.sql.entity;

import com.rackspace.idm.domain.entity.KeyMetadata;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.util.Date;

@Data
@Entity
@Table(name = "keyczar_metadata_rax")
public class SqlKeyMetadata implements KeyMetadata {

    @Id
    @Column(name = "id", length = 64)
    private String name;

    @Column(name = "created")
    @Temporal(TemporalType.TIMESTAMP)
    private Date created = new Date();

    @Column(name = "data")
    private String data;
}
