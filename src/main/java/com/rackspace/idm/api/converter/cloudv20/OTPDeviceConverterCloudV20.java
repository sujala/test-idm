package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.OTPDevice;
import com.rackspace.idm.util.OTPHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
        device.setVerified(entity.getMultiFactorDeviceVerified() == null ? false : entity.getMultiFactorDeviceVerified());
        return device;
    }

}
