package testHelpers;

import com.rackspace.idm.helpers.WiserWrapper;

public class SingletonWiserEmailServer {

    private static WiserWrapper instance = WiserWrapper.startWiser(10026);

    private SingletonWiserEmailServer() {

    }

    public static WiserWrapper getInstance() {
        return instance;
    }

    @SuppressWarnings("unchecked")
    public synchronized void reset() {
        instance.getWiser().getMessages().clear();
    }

}
