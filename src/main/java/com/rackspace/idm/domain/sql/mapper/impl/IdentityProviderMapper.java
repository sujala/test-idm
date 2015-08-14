package com.rackspace.idm.domain.sql.mapper.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.entity.IdentityProvider;
import com.rackspace.idm.domain.sql.entity.SqlIdentityProvider;
import com.rackspace.idm.domain.sql.entity.SqlUserCertificate;
import com.rackspace.idm.domain.sql.mapper.SqlMapper;

import java.util.ArrayList;
import java.util.UUID;

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

    @Override
    public SqlIdentityProvider toSQL(IdentityProvider identityProvider) {
        if(identityProvider == null) {
            return null;
        }

        SqlIdentityProvider sqlIdentityProvider = super.toSQL(identityProvider);

        for(byte[] cert : identityProvider.getUserCertificates()) {
            SqlUserCertificate sqlCert = new SqlUserCertificate();
            sqlCert.setCertificate(cert);
            sqlCert.setIdentityProvider(sqlIdentityProvider);
            sqlCert.setUuid(getNextId());
            sqlIdentityProvider.getUserCertificates().add(sqlCert);
        }

        return sqlIdentityProvider;
    }

    public static String getNextId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

}
