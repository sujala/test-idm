package testHelpers;

import com.rackspace.identity.multifactor.providers.UserManagement;
import com.rackspace.identity.multifactor.providers.duo.domain.DuoPhone;
import com.rackspace.identity.multifactor.providers.duo.domain.DuoUser;
import com.rackspace.idm.Constants;
import lombok.experimental.Delegate;

import static org.mockito.Mockito.*;

public class SingletonMockUserManagement implements UserManagement<DuoUser, DuoPhone> {

    private static SingletonMockUserManagement instance = new SingletonMockUserManagement();

    @Delegate
    private UserManagement<DuoUser, DuoPhone> mock;

    private SingletonMockUserManagement() {
        reset();
    }

    public static SingletonMockUserManagement getInstance() {
        return instance;
    }

    @SuppressWarnings("unchecked")
    public synchronized void reset() {
        mock = mock(UserManagement.class);
        DuoUser user = new DuoUser();
        user.setUser_id(Constants.MFA_DEFAULT_USER_PROVIDER_ID);
        DuoPhone phone = new DuoPhone();
        phone.setPhone_id(Constants.MFA_DEFAULT_PHONE_PROVIDER_ID);
        when(mock.createUser(any(DuoUser.class))).thenReturn(user);
        when(mock.getUserById(Constants.MFA_DEFAULT_USER_PROVIDER_ID)).thenReturn(user);
        when(mock.linkMobilePhoneToUser(anyString(), any(DuoPhone.class))).thenReturn(phone);
        when(mock.getPhoneById(Constants.MFA_DEFAULT_PHONE_PROVIDER_ID)).thenReturn(phone);
        String[] codes1 = {Constants.MFA_DEFAULT_BYPASS_CODE_1};
        String[] codes2 = {Constants.MFA_DEFAULT_BYPASS_CODE_1, Constants.MFA_DEFAULT_BYPASS_CODE_2};
        when(mock.getBypassCodes(eq(Constants.MFA_DEFAULT_USER_PROVIDER_ID), eq(1), anyInt())).thenReturn(codes1);
        when(mock.getBypassCodes(eq(Constants.MFA_DEFAULT_USER_PROVIDER_ID), eq(2), anyInt())).thenReturn(codes2);
    }

}
