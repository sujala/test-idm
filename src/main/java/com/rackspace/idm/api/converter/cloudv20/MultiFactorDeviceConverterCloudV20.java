package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhones;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.MultiFactorDevices;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.OTPDevices;
import com.rackspace.idm.domain.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MultiFactorDeviceConverterCloudV20 {

    @Autowired
    private OTPDeviceConverterCloudV20 otpDeviceConverterCloudV20;

    @Autowired
    private MobilePhoneConverterCloudV20 mobilePhoneConverterCloudV20;

    public MultiFactorDevices toMultiFactorDevicesForWeb(List<com.rackspace.idm.domain.entity.MultiFactorDevice> entityList, User user) {
        MultiFactorDevices devices = new MultiFactorDevices();

        OTPDevices otpDevices = new OTPDevices();
        MobilePhones mobilePhones = new MobilePhones();

        for (com.rackspace.idm.domain.entity.MultiFactorDevice multiFactorDevice : entityList) {
            //ideally would dynamically load converters, applicable to converting MFA devices. But not necessary right now...
            if (multiFactorDevice instanceof com.rackspace.idm.domain.entity.OTPDevice) {
                otpDevices.getOtpDevice().add(otpDeviceConverterCloudV20.toOTPDeviceForWeb((com.rackspace.idm.domain.entity.OTPDevice) multiFactorDevice));
            } else if (multiFactorDevice instanceof com.rackspace.idm.domain.entity.MobilePhone) {
                mobilePhones.getMobilePhone().add(mobilePhoneConverterCloudV20.toMobilePhoneWebIncludingVerifiedFlag((com.rackspace.idm.domain.entity.MobilePhone) multiFactorDevice, user));
            }
        }

        devices.setOtpDevices(otpDevices);
        devices.setMobilePhones(mobilePhones);

        return devices;
    }
}
