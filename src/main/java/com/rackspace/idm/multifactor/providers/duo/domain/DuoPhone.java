package com.rackspace.idm.multifactor.providers.duo.domain;

import com.rackspace.idm.multifactor.providers.ProviderPhone;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 */
@Getter
@Setter
public class DuoPhone implements ProviderPhone {
    private String phone_id;
    private String number;
    private String name;
    private String extension;
    private List<DuoUser> users;

    @Override
    public String getProviderId() {
        return phone_id;
    }
}
