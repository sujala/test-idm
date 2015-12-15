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

    private static final String FORMAT = "ou=%s,o=externalProviders,dc=rackspace,dc=com";

    @Override
    protected String getUniqueIdFormat() {
        return FORMAT;
    }

    @Override
    protected String[] getIds(SqlIdentityProvider sqlIdentityProvider) {
        return new String[] {sqlIdentityProvider.getName()};
    }

    @Override
    public IdentityProvider fromSQL(SqlIdentityProvider sqlEntity, IdentityProvider identityProvider, boolean ignoreNulls) {
        if (sqlEntity == null) {
            return null;
        }

        identityProvider = super.fromSQL(sqlEntity, identityProvider, ignoreNulls);
        identityProvider.setUserCertificates(new ArrayList<byte[]>());

        if (sqlEntity.getUserCertificates() != null) {
            for (SqlUserCertificate userCertificate : sqlEntity.getUserCertificates()) {
                identityProvider.getUserCertificates().add(userCertificate.getCertificate());
            }
        }

        identityProvider.setFederationType(sqlEntity.getTargetUserSource());

        //set the list of approved domains to null if it is empty. This is to match the LDAP implementation and mapping
        if (identityProvider.getApprovedDomainIds() != null && identityProvider.getApprovedDomainIds().isEmpty()) {
            identityProvider.setApprovedDomainIds(null);
        }

        return identityProvider;
    }

    @Override
    public SqlIdentityProvider toSQL(IdentityProvider identityProvider, SqlIdentityProvider sqlIdentityProvider, boolean ignoreNulls) {
        if(identityProvider == null) {
            return null;
        }

        sqlIdentityProvider = super.toSQL(identityProvider, sqlIdentityProvider, ignoreNulls);

        if (identityProvider.getUserCertificates() != null) {
            for(byte[] cert : identityProvider.getUserCertificates()) {
                SqlUserCertificate sqlCert = new SqlUserCertificate();
                sqlCert.setCertificate(cert);
                sqlCert.setIdentityProvider(sqlIdentityProvider);
                sqlCert.setUuid(getNextId());
                sqlIdentityProvider.getUserCertificates().add(sqlCert);
            }
        }

        return sqlIdentityProvider;
    }

    public static String getNextId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

}
