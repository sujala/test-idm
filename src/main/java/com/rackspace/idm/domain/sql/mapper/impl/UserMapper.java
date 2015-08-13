package com.rackspace.idm.domain.sql.mapper.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.EncryptionService;
import com.rackspace.idm.domain.sql.dao.GroupRepository;
import com.rackspace.idm.domain.sql.entity.SqlUser;
import com.rackspace.idm.domain.sql.entity.SqlUserRax;
import com.rackspace.idm.domain.sql.mapper.SqlRaxMapper;
import com.rackspace.idm.util.CryptHelper;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.List;

@SQLComponent
public class UserMapper extends SqlRaxMapper<User, SqlUser, SqlUserRax> {

    private static final String FORMAT = "rsId=%s,ou=users,o=rackspace,dc=rackspace,dc=com";

    @Autowired
    private EncryptionService encryptionService;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private CryptHelper cryptHelper;

    @Override
    public User fromSQL(SqlUser sqlUser, boolean ignoreNulls) {
        if (sqlUser == null) {
            return null;
        }

        final User user = super.fromSQL(sqlUser, ignoreNulls);
        if (user.getUniqueId() == null) {
            user.setUniqueId(fromSqlUserToUniqueId(sqlUser));
        }

        encryptionService.decryptUser(user);
        user.setPasswordIsNew(false);
        user.setPassword(null);
        return user;
    }

    @Override
    public SqlUser toSQL(User user, SqlUser sqlUser, boolean ignoreNulls) {
        if (user == null) {
            return null;
        }

        encryptionService.encryptUser(user);
        sqlUser = super.toSQL(user, sqlUser, ignoreNulls);
        if (user.getUniqueId() == null) {
            user.setUniqueId(fromSqlUserToUniqueId(sqlUser));
        }
        if (user.getPassword() != null && (
                !user.getPassword().equals(user.getUserPassword()) ||
                !cryptHelper.isPasswordEncrypted(user.getPassword()))) {
            // TODO: sqlUser.setUserPassword(Crypt.crypt(user.getPassword()));
            sqlUser.setUserPassword(cryptHelper.createLegacySHA(user.getPassword()));
        }
        return sqlUser;
    }

    public List<String> getExtraAttributes() {
        return Arrays.asList("email");
    }

    private String fromSqlUserToUniqueId(SqlUser sqlUser) {
        if (sqlUser != null) {
            return String.format(FORMAT, sqlUser.getId());
        }
        return null;
    }

}