package com.rackspace.idm.domain.security;

import com.rackspace.idm.domain.entity.BaseUser;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class ConfigurableTokenFormatSelector implements TokenFormatSelector {

    @Override
    public TokenFormat formatForNewToken(BaseUser user) {
        return TokenFormat.AE;
    }

    @Override
    public TokenFormat formatForExistingToken(String accessToken) {
        return TokenFormat.AE;
    }
}
