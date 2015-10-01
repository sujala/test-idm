package com.rackspace.idm.domain.migration.dao.impl;

import com.rackspace.idm.annotation.MigrationComponent;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.impl.LdapConnectionPools;
import com.rackspace.idm.domain.migration.ChangeType;
import com.rackspace.idm.domain.migration.dao.DeltaDao;
import com.rackspace.idm.domain.migration.ldap.entity.LdapToSqlEntity;
import com.unboundid.ldap.sdk.LDAPInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@MigrationComponent
@Repository
public class LdapDeltaRepository implements DeltaDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(LdapDeltaRepository.class);

    private static final String EVENT_INSERT_SQL = "insert into delta_ldap_to_sql_rax (id, event, host, type, data) values (:id, :event, :host, :type, :data)";
    private static final String EVENT_DELETE_ALL_SQL = "delete from delta_ldap_to_sql_rax";
    private static final String EVENT_SELECT_ALL_BY_TYPE_SQL = "select * from delta_ldap_to_sql_rax where type = :type ORDER BY created";

    @Autowired
    private IdentityConfig identityConfig;

    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    private LdapConnectionPools connPools;

    private LDAPInterface getAppInterface() {
        return connPools.getAppConnPoolInterface();
    }

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    @Transactional
    public void save(ChangeType event, String type, String ldif) {
        try {
            Map namedParameters = new HashMap();
            namedParameters.put("id", getId());
            namedParameters.put("event", event.name());
            namedParameters.put("type", type);
            namedParameters.put("host", identityConfig.getReloadableConfig().getAENodeNameForSignoff());

            //ignoring what is passed in 'ldif' parameter and pulling fresh from CA
            String refreshedLDIF = null;
            if (event != ChangeType.DELETE) {
                refreshedLDIF = getAppInterface().getEntry(type).toLDIFString();
            }
            namedParameters.put("data", refreshedLDIF);

            namedParameterJdbcTemplate.update(EVENT_INSERT_SQL, namedParameters);
        } catch (Exception e) {
            LOGGER.error("Cannot save delta (LDAP->SQL)!", e);
        }
    }

    private String getId() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    @Override
    public List<LdapToSqlEntity> findByType(String type) {
        SqlParameterSource namedParameters = new MapSqlParameterSource("type", type);

        List<LdapToSqlEntity> logs = this.namedParameterJdbcTemplate.query(
                EVENT_SELECT_ALL_BY_TYPE_SQL, namedParameters, new LdapToSqlEntityRowMapper());
        return logs;
    }

    @Override
    public void deleteAll() {
        namedParameterJdbcTemplate.getJdbcOperations().update(EVENT_DELETE_ALL_SQL);
    }

    private static class LdapToSqlEntityRowMapper implements RowMapper {
        public LdapToSqlEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
            LdapToSqlEntity entry = new LdapToSqlEntity();
            entry.setId(rs.getString("id"));
            entry.setEvent(ChangeType.valueOf(rs.getString("event")));
            entry.setType(rs.getString("type"));
            entry.setHost(rs.getString("host"));
            entry.setData(rs.getString("data"));
            entry.setCreated(rs.getTimestamp("created"));
            entry.setRetrieved(rs.getTimestamp("retrieved"));
            entry.setMigrated(rs.getTimestamp("migrated"));
            entry.setError(rs.getString("error"));
            return entry;
        }
    }

}
