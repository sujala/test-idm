package com.rackspace.idm.domain.sql.mapper.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.entity.OTPDevice;
import com.rackspace.idm.domain.sql.entity.SqlOTPDevice;
import com.rackspace.idm.domain.sql.mapper.SqlMapper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SQLComponent
public class OTPDeviceMapper extends SqlMapper<OTPDevice, SqlOTPDevice> {

    private static final String ID_FORMAT = "([a-z0-9]+)";
    private static final String FORMAT = "rsId=%s,cn=OTPDEVICES,rsId=%s,ou=users,o=rackspace,dc=rackspace,dc=com";
    private static final Pattern REGEXP = Pattern.compile(String.format(FORMAT, ID_FORMAT, ID_FORMAT));

    @Override
    protected String getUniqueIdFormat() {
        return FORMAT;
    }

    @Override
    protected String[] getIds(SqlOTPDevice sqlOTPDevice) {
        return new String[] {sqlOTPDevice.getId(), sqlOTPDevice.getUserId()};
    }

    @Override
    public SqlOTPDevice toSQL(OTPDevice otpDevice) {
        final SqlOTPDevice device = super.toSQL(otpDevice);
        if (device != null) {
            device.setUserId(fromUniqueIdToUserId(otpDevice.getUniqueId()));
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
