package com.rackspace.idm.domain.security;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.TokenFormatEnum;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.AuthDao;
import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.FederatedUser;
import com.rackspace.idm.domain.entity.Racker;
import com.rackspace.idm.domain.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Primary
@Component
public class ConfigurableTokenFormatSelector implements TokenFormatSelector {

    private static final Pattern UUID_PATTERN = Pattern.compile("[0-9a-fA-F]{8}-{0,1}[0-9a-fA-F]{4}-{0,1}[0-9a-fA-F]{4}-{0,1}[0-9a-fA-F]{4}-{0,1}[0-9a-fA-F]{12}");

    @Autowired
    private IdentityConfig identityConfig;

    @Autowired
    private AuthDao ldapAuthRepository;

    @Override
    public TokenFormat formatForNewToken(BaseUser user) {
        if (!identityConfig.getReloadableConfig().getFeatureAETokensEncrypt()) {
            return TokenFormat.UUID;
        }

        // Provisioned user
        if (user instanceof User) {
            final TokenFormatEnum userSpecified = ((User)user).getTokenFormatAsEnum();
            if (userSpecified == null || userSpecified == TokenFormatEnum.DEFAULT) {
                return identityConfig.getIdentityProvisionedTokenFormat();
            }
            switch (userSpecified) {
                case AE:
                    return TokenFormat.AE;
                case UUID:
                    return TokenFormat.UUID;
                default:
                    return identityConfig.getIdentityProvisionedTokenFormat();
            }
        }

        // Racker user
        if (user instanceof Racker) {
            Racker racker = (Racker) user;

            //if federated racker request
            if (racker.isFederatedRacker()) {
                return identityConfig.getReloadableConfig().getIdentityFederationRequestTokenFormatForIdp(racker.getFederatedIdpUri());
            } else {
                return TokenFormat.AE;
            }
        }

        //if federated user
        if (user instanceof FederatedUser) {
            final FederatedUser federatedUser = (FederatedUser) user;
            return identityConfig.getReloadableConfig().getIdentityFederationRequestTokenFormatForIdp(federatedUser.getFederatedIdpUri());
        }

        return TokenFormat.AE;
    }

    @Override
    public TokenFormat formatForExistingToken(String accessToken) {
        if (UUID_PATTERN.matcher(accessToken).matches()) {
            return TokenFormat.UUID;
        } else {
            /*
            we now consider any token that doesn't match UUID format to be an AE token.
             */
            return TokenFormat.AE;
        }
    }
}
