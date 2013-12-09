package com.rackspace.idm.multifactor.domain;

/**
 * A string based immutable pin
 */
public class BasicPin implements Pin {
    private String pin;

    public BasicPin(String pin) {
        this.pin = pin;
    }

    @Override
    public String getPin() {
        return pin;
    }
}
