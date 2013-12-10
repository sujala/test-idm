package com.rackspace.idm.multifactor.providers.duo.domain;

import com.rackspace.idm.multifactor.domain.Pin;

/**
 */
public class DuoPin implements Pin {
    private String pin;

    public DuoPin(String pin) {
        this.pin = pin;
    }

    @Override
    public String getPin() {
        return pin;
    }

}
