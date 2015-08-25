package com.rackspace.idm.domain.sql.mapper.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.entity.BypassDevice;
import com.rackspace.idm.domain.sql.entity.SqlBypassDevice;
import com.rackspace.idm.domain.sql.mapper.SqlMapper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SQLComponent
public class BypassDeviceMapper extends SqlMapper<BypassDevice, SqlBypassDevice> {

    private static final String ID_FORMAT = "([a-z0-9]+)";
    private static final String FORMAT = "rsId=%s,cn=BYPASSCODES,rsId=%s,ou=users,o=rackspace,dc=rackspace,dc=com";
    private static final Pattern REGEXP = Pattern.compile(String.format(FORMAT, ID_FORMAT, ID_FORMAT));

    @Override
    protected String getUniqueIdFormat() {
        return FORMAT;
    }

    @Override
    protected Object[] getIds(SqlBypassDevice sqlBypassDevice) {
        return new Object[] {sqlBypassDevice.getId(), sqlBypassDevice.getUserId()};
    }

    @Override
    public SqlBypassDevice toSQL(BypassDevice bypassDevice) {
        final SqlBypassDevice device = super.toSQL(bypassDevice);
        if (device != null) {
            device.setUserId(fromUniqueIdToUserId(bypassDevice.getUniqueId()));
        }
        return device;
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
