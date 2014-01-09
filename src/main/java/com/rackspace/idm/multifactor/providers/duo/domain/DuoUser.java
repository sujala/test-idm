package com.rackspace.idm.multifactor.providers.duo.domain;

import com.rackspace.idm.multifactor.providers.ProviderUser;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 */
@Getter
@Setter
public class DuoUser implements ProviderUser {
    private String email;
    private String realname;
    private String status;
    private String user_id;
    private String username;
    private List<DuoPhone> phones;

    @Override
    public String getProviderId() {
        return user_id;
    }

    public UserStatus getStatusAsEnum() {
        return UserStatus.fromDuoSecurityCode(status);
    }

}
