package testHelpers;

import lombok.Data;
import lombok.Delegate;
import org.apache.commons.configuration.Configuration;

import java.util.HashMap;
import java.util.Map;

/*
ConfigurationWrapper will override values on the base.idm.properties and idm.properties keys.
This will allow for running integration test without restarting grizzly with new config vales.
 */

@Data
public class ConfigurationWrapper implements Configuration {

    interface Exclude {
        boolean getBoolean(String key);
        boolean getBoolean(String key, boolean defaultValue);
        String getString(String key);
    }

    public static Map<String, Object> overrideData = new HashMap<String, Object>();

    @Delegate(types = Configuration.class, excludes = Exclude.class)
    Configuration config;

    @Override
    public boolean getBoolean(String key) {
        if (overrideData.containsKey(key)) {
            return (Boolean) overrideData.get(key);
        }
        return config.getBoolean(key);
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        if (overrideData.containsKey(key)) {
            return (Boolean) overrideData.get(key);
        }
        return config.getBoolean(key, defaultValue);
    }

    @Override
    public String getString(String key) {
        if (overrideData.containsKey(key)) {
            return (String) overrideData.get(key);
        }
        return config.getString(key);
    }
}
