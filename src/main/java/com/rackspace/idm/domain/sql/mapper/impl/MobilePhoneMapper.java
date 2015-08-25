package com.rackspace.idm.domain.sql.mapper.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.entity.MobilePhone;
import com.rackspace.idm.domain.sql.entity.SqlMobilePhone;
import com.rackspace.idm.domain.sql.mapper.SqlMapper;

@SQLComponent
public class MobilePhoneMapper extends SqlMapper<MobilePhone, SqlMobilePhone> {

    private static final String FORMAT = "telephoneNumber=%s,ou=mobilePhones,ou=multiFactorDevices,o=rackspace,dc=rackspace,dc=com";

    @Override
    protected String getUniqueIdFormat() {
        return FORMAT;
    }

    @Override
    protected Object[] getIds(SqlMobilePhone sqlMobilePhone) {
        return new Object[] {sqlMobilePhone.getTelephoneNumber().replaceFirst("\\+", "\\\\+")};
    }

}
