package testHelpers;

import com.rackspace.idm.Constants;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.config.PropertyFileConfiguration;
import lombok.Delegate;
import org.apache.commons.configuration.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

/**
A JVM singleton wrapper around the property file configuration used in the IDM app. By using a JVM singleton we ensure
that all Spring application contexts loaded within the JVM will use the same underlying configuration. For example,
Grizzly tests use two app contexts (1 for grizzly, 1 for the test itself within spock). This allows a config property to
be changed in the spock test and affect the configuration used within grizzly.

 Note, however, that not all IDM properties can be dynamically changed AFTER the application has loaded. For example, the
 various properties to connect to LDAP are read and used to create connections on startup. Subsequently changing these
 properties will not have the desired affect.
 */
public final class SingletonConfiguration implements Configuration {

    private static SingletonConfiguration instance = new SingletonConfiguration();

    @Delegate(types = Configuration.class)
    private Configuration idmPropertiesConfig;

    private PropertyFileConfiguration pfConfig = new PropertyFileConfiguration();

    private SingletonConfiguration(){
        reset();
    }

    public static SingletonConfiguration getInstance() {return instance;}

    /**
     * Reload the configuration from scratch. This will reset any changed properties back to the default values.
     */
    public synchronized void reset() {
        idmPropertiesConfig = pfConfig.getConfig();

        //Regardless of setting, tests require the Classpath MFA keys (for legacy)
        try {
            ClassPathResource resource = new ClassPathResource(Constants.TEST_KEYS_LOCATION);
            String pathLocation = resource.getFile().getAbsolutePath();
            idmPropertiesConfig.setProperty(IdentityConfig.MULTIFACTOR_ENCRYPTION_KEY_LOCATION_PROP_NAME, pathLocation);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //set the wiser email info
        idmPropertiesConfig.setProperty(IdentityConfig.EMAIL_HOST, SingletonWiserEmailServer.getInstance().getHost());
        idmPropertiesConfig.setProperty(IdentityConfig.EMAIL_PORT, String.valueOf(SingletonWiserEmailServer.getInstance().getPort()));
    }
}
