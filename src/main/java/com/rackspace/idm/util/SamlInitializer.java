package com.rackspace.idm.util;

import org.opensaml.DefaultBootstrap;
import org.opensaml.xml.ConfigurationException;

/**
 * Overriding DefaultBootstrap because we use velocity for our app and need a newer version. OpenSAML uses velocity for
 * features we don't need so don't want to use it (or have opensaml initialize velocity). Only way to do this is override
 * the class. See  <a href="http://shibboleth.1660669.n2.nabble.com/OpenSAML-dependency-on-Velocity-breaks-application-td7505286.html">
 *     http://shibboleth.1660669.n2.nabble.com/OpenSAML-dependency-on-Velocity-breaks-application-td7505286.html</a>
 */
public class SamlInitializer extends DefaultBootstrap {
    /** List of default XMLTooling configuration files. */
    private static String[] xmlToolingConfigs = {
            "/default-config.xml",
            "/schema-config.xml",
            "/signature-config.xml",
            "/signature-validation-config.xml",
            "/encryption-config.xml",
            "/encryption-validation-config.xml",
            "/soap11-config.xml",
            "/wsfed11-protocol-config.xml",
            "/saml1-assertion-config.xml",
            "/saml1-protocol-config.xml",
            "/saml1-core-validation-config.xml",
            "/saml2-assertion-config.xml",
            "/saml2-protocol-config.xml",
            "/saml2-core-validation-config.xml",
            "/saml1-metadata-config.xml",
            "/saml2-metadata-config.xml",
            "/saml2-metadata-validation-config.xml",
            "/saml2-metadata-attr-config.xml",
            "/saml2-metadata-idp-discovery-config.xml",
            "/saml2-metadata-ui-config.xml",
            "/saml2-protocol-thirdparty-config.xml",
            "/saml2-metadata-query-config.xml",
            "/saml2-assertion-delegation-restriction-config.xml",
            "/saml2-ecp-config.xml",
            "/xacml10-saml2-profile-config.xml",
            "/xacml11-saml2-profile-config.xml",
            "/xacml20-context-config.xml",
            "/xacml20-policy-config.xml",
            "/xacml2-saml2-profile-config.xml",
            "/xacml3-saml2-profile-config.xml",
            "/wsaddressing-config.xml",
            "/wssecurity-config.xml",
    };

    /** Constructor. */
    protected SamlInitializer() {

    }

    /**
     * Initializes the OpenSAML library, loading default configurations.
     *
     * @throws ConfigurationException thrown if there is a problem initializing the OpenSAML library
     */
    public static synchronized void bootstrap() throws ConfigurationException {
        initializeXMLSecurity();
        initializeXMLTooling(xmlToolingConfigs);
        initializeArtifactBuilderFactories();
        initializeGlobalSecurityConfiguration();
        initializeParserPool();
        initializeESAPI();
    }
}
