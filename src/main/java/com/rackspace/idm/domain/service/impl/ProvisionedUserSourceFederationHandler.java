package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperClient;
import com.rackspace.idm.api.resource.cloud.atomHopper.FeedsUserStatusEnum;
import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.decorator.LogoutRequestDecorator;
import com.rackspace.idm.domain.decorator.SamlResponseDecorator;
import com.rackspace.idm.domain.entity.FederatedUser;
import com.rackspace.idm.domain.entity.IdentityProvider;
import com.rackspace.idm.domain.entity.SamlAuthResponse;
import com.rackspace.idm.domain.entity.SamlLogoutResponse;
import com.rackspace.idm.domain.service.IdentityUserService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.util.SamlLogoutResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Handles SAML authentication requests against provisioned users. This means against a particular domain.
 *
 * @deprecated v1.0 Fed API is deprecated and should no longer ever be used.
 */
@Deprecated
@Component
public class ProvisionedUserSourceFederationHandler implements ProvisionedUserFederationHandler {
    private static final Logger log = LoggerFactory.getLogger(DefaultFederatedIdentityService.class);

    @Autowired
    IdentityUserService identityUserService;

    @Autowired
    private AtomHopperClient atomHopperClient;

    @Override
    public SamlAuthResponse processRequestForProvider(SamlResponseDecorator samlResponseDecorator, IdentityProvider provider) {
        return null;
    }

    @Override
    public SamlLogoutResponse processLogoutRequestForProvider(LogoutRequestDecorator logoutRequestDecorator, IdentityProvider provider) {
        String username = logoutRequestDecorator.checkAndGetUsername();

        FederatedUser user = identityUserService.getFederatedUserByUsernameAndIdentityProviderId(username, provider.getProviderId());

        if (user == null) {
            log.error(String.format("Unable to process federated user logout request. Domain federated user %s does not exist for provider %s.", username, provider.getProviderId()));
            throw new BadRequestException("Not Found");
        }

        identityUserService.deleteUser(user);

        //send atom hopper feed showing deletion of this user
        atomHopperClient.asyncPost(user, FeedsUserStatusEnum.DELETED, MDC.get(Audit.GUUID));

        return SamlLogoutResponseUtil.createSuccessfulLogoutResponse(logoutRequestDecorator.getLogoutRequest().getID());
    }
}
