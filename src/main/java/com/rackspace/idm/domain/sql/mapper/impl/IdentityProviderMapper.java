package com.rackspace.idm.domain.sql.mapper.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.entity.IdentityProvider;
import com.rackspace.idm.domain.sql.entity.SqlIdentityProvider;
import com.rackspace.idm.domain.sql.entity.SqlUserCertificate;
import com.rackspace.idm.domain.sql.mapper.SqlMapper;

import java.util.ArrayList;

@SQLComponent
public class IdentityProviderMapper extends SqlMapper<IdentityProvider, SqlIdentityProvider> {

    public IdentityProvider fromSQL(SqlIdentityProvider sqlEntity) {
        if (sqlEntity == null) {
            return null;
        }

        IdentityProvider identityProvider = super.fromSQL(sqlEntity);
        identityProvider.setUserCertificates(new ArrayList<byte[]>());

        for (SqlUserCertificate userCertificate : sqlEntity.getUserCertificates()) {
            identityProvider.getUserCertificates().add(userCertificate.getCertificate());
        }

        return identityProvider;
    }

}
