package com.rackspace.idm.util;

import com.rackspace.idm.domain.entity.BypassDevice;
import lombok.Getter;

import java.util.Collections;
import java.util.Set;

@Getter
public final class BypassDeviceCreationResult {
    private BypassDevice device;
    private Set<String> plainTextBypassCodes;

    public BypassDeviceCreationResult(BypassDevice device, Set<String> plainTextBypassCodes) {
        this.device = device;
        this.plainTextBypassCodes = Collections.unmodifiableSet(plainTextBypassCodes);
    }
}
