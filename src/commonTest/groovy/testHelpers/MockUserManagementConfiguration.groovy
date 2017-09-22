package testHelpers

import com.rackspace.identity.multifactor.providers.UserManagement
import com.rackspace.identity.multifactor.providers.duo.domain.DuoPhone
import com.rackspace.identity.multifactor.providers.duo.domain.DuoUser
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class MockUserManagementConfiguration {

    @Bean
    @Primary
    public UserManagement<DuoUser, DuoPhone> createMock() {
        return SingletonMockUserManagement.getInstance()
    }

}
