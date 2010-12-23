package configuration;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

public class IdmServiceConfiguration {

    private org.apache.commons.configuration.Configuration config;

    public IdmServiceConfiguration() {
        try {
            config = new PropertiesConfiguration("configuration/IdmService.properties");
        } catch (ConfigurationException e) {
            System.out.println(e);
            throw new IllegalStateException("Could not load CustomerDb configuration", e);
        }
    }

    public String getServerAddress() {
         String val = config.getString("server.address");
         return val;
    }

    public String getServerRequestType() {
         String val = config.getString("server.requestType");
         return val;
    }

    public String getApiClientId() {
         String val = config.getString("apiClient.id");
         return val;
    }

    public String getApiClientSecret() {
         String val = config.getString("apiClient.secret");
         return val;
    }

    public String getUsersResourceUri() {
         String val = config.getString("users.resourceUri");
         return val;
    }

    public String getTokensResourceUri() {
         String val = config.getString("tokens.resourceUri");
         return val;
    }
}
