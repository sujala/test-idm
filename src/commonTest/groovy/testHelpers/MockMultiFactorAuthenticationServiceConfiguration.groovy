package testHelpers

import com.rackspace.identity.multifactor.providers.MultiFactorAuthenticationService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class MockMultiFactorAuthenticationServiceConfiguration {

    @Bean
    @Primary
    public MultiFactorAuthenticationService getMfaAuthService() {
        return SingletonMockMultiFactorAuthenticationService.getInstance();
    }
}
