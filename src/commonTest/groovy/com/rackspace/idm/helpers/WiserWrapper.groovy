package com.rackspace.idm.helpers

import lombok.Getter
import org.apache.log4j.Logger
import org.subethamail.smtp.auth.EasyAuthenticationHandlerFactory
import org.subethamail.smtp.auth.LoginFailedException
import org.subethamail.smtp.auth.UsernamePasswordValidator
import org.subethamail.wiser.Wiser
import org.subethamail.wiser.WiserMessage

@Getter
class WiserWrapper {
    private static final Logger LOGGER = Logger.getLogger(WiserWrapper.class);
    public static final int DEFAULT_NUM_RETRIES = 10;

    def Wiser wiserServer

    private WiserWrapper(Wiser wiserServer) {
        this.wiserServer = wiserServer
    }

    def String getHost() {
        return wiserServer.getServer().getHostName()
    }

    def int getPort() {
        return wiserServer.getServer().getPort()
    }

    def Wiser getWiser() {
        return wiserServer
    }

    public static WiserWrapper startWiser(int startPort) {
        return startWiser(startPort, DEFAULT_NUM_RETRIES);
    }

    public static WiserWrapper startWiser(int startPort, int retries) {
        int count = 0;
        Throwable lastException = null;
        def host = "localhost"
        while (count < retries) {
            def port = startPort + count
            try {
                UsernamePasswordValidator validator = new UsernamePasswordValidator() {
                    @Override
                    public void login(String username, String password) throws LoginFailedException {

                    }
                };

                Wiser wiser = new Wiser();
                wiser.getServer().setAuthenticationHandlerFactory(new EasyAuthenticationHandlerFactory(validator));
                wiser.setPort(port);
                wiser.setHostname(host);
                wiser.start();

                return new WiserWrapper(wiser);
            } catch (RuntimeException ex) {
                lastException = ex;
                count++;
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Error starting wiser. Retrying with higher port until maximum attempts reached", ex);
                }
            }
        }
        throw new IllegalStateException("Could not start wiser on a port. Kept getting startup failures. Including last exception as cause.", lastException);
    }
}
