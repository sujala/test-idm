package testHelpers;

import com.rackspace.identity.multifactor.providers.MultiFactorAuthenticationService;
import lombok.Delegate;

import static org.mockito.Mockito.*;

public class SingletonMockMultiFactorAuthenticationService implements MultiFactorAuthenticationService {

    private static SingletonMockMultiFactorAuthenticationService instance = new SingletonMockMultiFactorAuthenticationService();

    @Delegate(types = MultiFactorAuthenticationService.class)
    private MultiFactorAuthenticationService mock;

    private SingletonMockMultiFactorAuthenticationService() {
        reset();
    }

    public static SingletonMockMultiFactorAuthenticationService getInstance() {
        return instance;
    }

    public synchronized void reset() {
        mock = mock(MultiFactorAuthenticationService.class);
    }

}
