package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhones;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.OTPDevice;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.OTPDevices;
import com.rackspace.idm.domain.entity.EndUser;
import com.rackspace.idm.util.OTPHelper;
import org.openstack.docs.identity.api.v2.ObjectFactory;
import org.openstack.docs.identity.api.v2.UserList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class OTPDeviceConverterCloudV20 {

    @Autowired
    private OTPHelper otpHelper;

    public OTPDevice toOTPDeviceForCreate(com.rackspace.idm.domain.entity.OTPDevice entity) {
        final OTPDevice device = toOTPDeviceForWeb(entity);
        final String keyUri = otpHelper.fromKeyToURI(entity.getKey(), entity.getName());
        device.setKeyUri(keyUri);
        device.setQrcode(otpHelper.fromStringToQRCode(keyUri));
        return device;
    }

    public OTPDevice toOTPDeviceForWeb(com.rackspace.idm.domain.entity.OTPDevice entity) {
        final OTPDevice device = new OTPDevice();
        device.setName(entity.getName());
        device.setId(entity.getId());
        device.setVerified(entity.getMultiFactorDeviceVerified() == null ? false : entity.getMultiFactorDeviceVerified());
        return device;
    }


    public OTPDevices toOTPDevicesForWeb(List<com.rackspace.idm.domain.entity.OTPDevice> entityList) {
        OTPDevices devices = new OTPDevices();
        for (com.rackspace.idm.domain.entity.OTPDevice otpDeviceEntity : entityList) {
            devices.getOtpDevice().add(toOTPDeviceForWeb(otpDeviceEntity));
        }
        return devices;
    }
}
