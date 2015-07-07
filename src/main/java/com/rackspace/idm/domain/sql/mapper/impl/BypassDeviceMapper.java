package com.rackspace.idm.domain.sql.mapper.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.entity.BypassDevice;
import com.rackspace.idm.domain.sql.entity.SqlBypassCode;
import com.rackspace.idm.domain.sql.entity.SqlBypassDevice;
import com.rackspace.idm.domain.sql.mapper.SqlMapper;

import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SQLComponent
public class BypassDeviceMapper extends SqlMapper<BypassDevice, SqlBypassDevice> {

    private static final String ID_FORMAT = "([a-z0-9]+)";
    private static final String FORMAT = "rsId=%s,cn=BYPASSCODES,rsId=%s,ou=users,o=rackspace,dc=rackspace,dc=com";
    private static final Pattern REGEXP = Pattern.compile(String.format(FORMAT, ID_FORMAT, ID_FORMAT));

    @Override
    public SqlBypassDevice toSQL(BypassDevice bypassDevice) {
        final SqlBypassDevice device = super.toSQL(bypassDevice);
        if (device != null) {
            device.setCodes(new HashSet<SqlBypassCode>());
            device.setUserId(fromUniqueIdToUserId(bypassDevice.getUniqueId()));
            for (String code : bypassDevice.getBypassCodes()) {
                final SqlBypassCode sqlCode = new SqlBypassCode();
                sqlCode.setId(device.getId());
                sqlCode.setCode(code);
                device.getCodes().add(sqlCode);
            }
        }
        return device;
    }

    @Override
    public BypassDevice fromSQL(SqlBypassDevice sqlBypassDevice) {
        final BypassDevice device = super.fromSQL(sqlBypassDevice);
        if (device != null) {
            device.setBypassCodes(new HashSet<String>());
            device.setUniqueId(fromSqlBypassDeviceToUniqueId(sqlBypassDevice));
            for (SqlBypassCode code : sqlBypassDevice.getCodes()) {
                device.getBypassCodes().add(code.getCode());
            }
        }
        return device;
    }

    public String fromSqlBypassDeviceToUniqueId(SqlBypassDevice device) {
        if (device != null) {
            return String.format(FORMAT, device.getId(), device.getUserId());
        }
        return null;
    }

    private String fromUniqueIdToUserId(String uniqueId) {
        if (uniqueId != null) {
            final Matcher matcher = REGEXP.matcher(uniqueId);
            if (matcher.matches() && matcher.groupCount() > 1) {
                return matcher.group(2);
            }
        }
        return null;
    }

}
