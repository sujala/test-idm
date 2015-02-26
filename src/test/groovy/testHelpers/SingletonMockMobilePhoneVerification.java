package testHelpers;

import com.google.i18n.phonenumbers.Phonenumber;
import com.rackspace.identity.multifactor.domain.BasicPin;
import com.rackspace.identity.multifactor.providers.MobilePhoneVerification;
import com.rackspace.idm.Constants;
import lombok.Delegate;

import static org.mockito.Mockito.*;

public class SingletonMockMobilePhoneVerification implements MobilePhoneVerification {

    private static SingletonMockMobilePhoneVerification instance = new SingletonMockMobilePhoneVerification();

    @Delegate(types = MobilePhoneVerification.class)
    private MobilePhoneVerification mock;

    private SingletonMockMobilePhoneVerification() {
        reset();
    }

    public static SingletonMockMobilePhoneVerification getInstance() {
        return instance;
    }

    public synchronized void reset() {
        mock = mock(MobilePhoneVerification.class);
        when(mock.sendPin(any(Phonenumber.PhoneNumber.class))).thenReturn(new BasicPin(Constants.MFA_DEFAULT_PIN));
    }

}
