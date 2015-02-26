package testHelpers

import com.rackspace.identity.multifactor.providers.MobilePhoneVerification
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class MockMobilePhoneVerificationConfiguration {

    @Bean
    @Primary
    public MobilePhoneVerification createMobilePhoneVerification() {
        return SingletonMockMobilePhoneVerification.getInstance()
    }

}
