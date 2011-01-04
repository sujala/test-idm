package com.rackspace.idm.authorizationService;

import junit.framework.TestCase;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rackspace.idm.services.DefaultUserService;

public class SunAuthorizationServiceTest extends TestCase {

    public SunAuthorizationService authorizationService;
    private Logger logger;
    AuthorizationTestHelper testHelper;

    protected void setUp() {
        this.logger = LoggerFactory.getLogger(DefaultUserService.class);

        String propsFileLoc = "";
        org.apache.commons.configuration.Configuration config = null;

        propsFileLoc = "SunXACMLAuthorization.properties";
        try {
            config = new PropertiesConfiguration(propsFileLoc);
        } catch (ConfigurationException e) {
            System.out.println(e);
            logger.error("Could not load Axiomatics configuraiton.", e);
        }

        String sunAuthConfigFilePath = config
            .getString("sunAuthConfigPathForTesting");
        String xacmlPolicyFilePath = config
            .getString("xacmlPolicyFilePathForTesting");
        String authLogFilePath = config.getString("logFilePathForTesting");

        boolean testMode = true;
        authorizationService = new SunAuthorizationService(logger,
            sunAuthConfigFilePath, xacmlPolicyFilePath, testMode);
        testHelper = new AuthorizationTestHelper(authorizationService);
    }

    public void testAllAuthorizationCases() {
        testHelper.testAllAuthorizations();
    }
}
