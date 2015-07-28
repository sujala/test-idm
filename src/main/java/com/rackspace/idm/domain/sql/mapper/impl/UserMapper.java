package com.rackspace.idm.domain.sql.mapper.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.EncryptionService;
import com.rackspace.idm.domain.sql.dao.GroupRepository;
import com.rackspace.idm.domain.sql.entity.SqlUser;
import com.rackspace.idm.domain.sql.entity.SqlUserRax;
import com.rackspace.idm.domain.sql.mapper.SqlRaxMapper;
import org.apache.commons.codec.digest.Crypt;
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

    @Override
    public User fromSQL(SqlUser sqlUser) {
        final User user = super.fromSQL(sqlUser);
        if (user.getUniqueId() == null) {
            user.setUniqueId(fromSqlUserToUniqueId(sqlUser));
        }
        encryptionService.encryptUser(user);
        return user;
    }

    @Override
    public SqlUser toSQL(User user) {
        encryptionService.decryptUser(user);
        final SqlUser sqlUser = super.toSQL(user);
        if (user.getUniqueId() == null) {
            user.setUniqueId(fromSqlUserToUniqueId(sqlUser));
        }
        if (user.getPassword() != null) {
            sqlUser.setUserPassword(Crypt.crypt(user.getPassword()));
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