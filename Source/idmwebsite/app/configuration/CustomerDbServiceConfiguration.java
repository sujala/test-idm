package configuration;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

public class CustomerDbServiceConfiguration {
    private org.apache.commons.configuration.Configuration config;

    public CustomerDbServiceConfiguration() {
        try {
            config = new PropertiesConfiguration("configuration/CustomerDbService.properties");
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

    public String getCustomerResourceUri() {
         String val = config.getString("customer.resourceUri");
         return val;
    }

    public String getPersonResourceUri() {
         String val = config.getString("person.resourceUri");
         return val;
    }
}
