package com.rackspace.idm.domain.sql.mapper.test;

import com.rackspace.idm.domain.dao.UniqueId;
import lombok.Data;

@Data
public class Dummy implements UniqueId {
    private Integer id;
    private String name;
    private String uniqueId;
}
