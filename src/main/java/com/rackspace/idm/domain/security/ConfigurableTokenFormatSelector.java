package com.rackspace.idm.domain.security;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.TokenFormatEnum;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Primary
@Component
public class ConfigurableTokenFormatSelector implements TokenFormatSelector {

    private static final Pattern UUID_PATTERN = Pattern.compile("[0-9a-fA-F]{8}-{0,1}[0-9a-fA-F]{4}-{0,1}[0-9a-fA-F]{4}-{0,1}[0-9a-fA-F]{4}-{0,1}[0-9a-fA-F]{12}-{0,1}");

    @Autowired
    private IdentityConfig identityConfig;

    @Override
    public TokenFormat formatForNewToken(BaseUser user) {
        if (user instanceof User) {
            final TokenFormatEnum userSpecified = ((User)user).getTokenFormatAsEnum();
            if (userSpecified == null || userSpecified == TokenFormatEnum.DEFAULT) {
                return getDefaultTokenFormat(user);
            }
            switch (userSpecified) {
                case AE:
                    return TokenFormat.AE;
                case UUID:
                    return TokenFormat.UUID;
                default:
                    getDefaultTokenFormat(user);
            }
        }
        return TokenFormat.UUID;
    }

    @Override
    public TokenFormat formatForExistingToken(String accessToken) {
        if (UUID_PATTERN.matcher(accessToken).matches()) {
            return TokenFormat.UUID;
        } else {
            return TokenFormat.AE;
        }
    }

    private TokenFormat getDefaultTokenFormat(BaseUser user) {
        //provisioned users...
        String strFormat = identityConfig.getIdentityProvisionedTokenFormat();

        //convert to format
        for (TokenFormat tokenFormat : TokenFormat.values()) {
            if (tokenFormat.name().equalsIgnoreCase(strFormat)) {
                return tokenFormat;
            }
        }
        return TokenFormat.UUID;
    }
}
