package com.rackspace.idm.authorizationService;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.Vector;

import org.slf4j.Logger;

import com.rackspace.idm.exceptions.ForbiddenException;
import com.rackspace.idm.exceptions.XACMLRequestCreationException;
import com.sun.xacml.ConfigurationStore;
import com.sun.xacml.Indenter;
import com.sun.xacml.PDP;
import com.sun.xacml.PDPConfig;
import com.sun.xacml.ParsingException;
import com.sun.xacml.UnknownIdentifierException;
import com.sun.xacml.attr.DateTimeAttribute;
import com.sun.xacml.attr.StringAttribute;
import com.sun.xacml.ctx.Attribute;
import com.sun.xacml.ctx.RequestCtx;
import com.sun.xacml.ctx.ResponseCtx;
import com.sun.xacml.ctx.Result;
import com.sun.xacml.ctx.Subject;
import com.sun.xacml.finder.PolicyFinder;
import com.sun.xacml.finder.PolicyFinderModule;

/*
 This Class is derived from SUN's XACML policy engine's TestDriver class.
 */
public class SunAuthorizationService implements AuthorizationService {
    // the pdp we use to do all evaluations
    private PDP pdp;

    // the module we use to manage all policy management
    private FilePolicyFinderModule policyModule;

    String sunAuthConfig;
    String policyLocationConfig;
    String policyPath;
    String authLogFilePath;
    private Logger logger;
    OutputStream logOutputStream = new ByteArrayOutputStream();

    public SunAuthorizationService(Logger logger, String sunConfigFilePath,
        String policyFilePath, boolean testMode) {

        this.sunAuthConfig = sunConfigFilePath;
        this.policyLocationConfig = policyFilePath;

        this.logger = logger;
        try {
            configurePDP(sunAuthConfig, policyLocationConfig, testMode);
        } catch (Exception exp) {
            logger
                .debug("(SunAuthorizationService) ----------- Error creating Authorization Service."
                    + "\n" + exp.toString());
            exp.printStackTrace();
            logger
                .debug("(SunAuthorizationService) ----------- Error creating Authorization Service.");
        }
    }

    public AuthorizationRequest createRequest(List<Entity> entities)
        throws XACMLRequestCreationException {

        if (entities == null) {
            throw new XACMLRequestCreationException("Request cannot be null.");
        }

        AuthorizationRequest authorizationRequest = null;

        Set<Subject> subjects = null;
        Set<com.sun.xacml.ctx.Attribute> resource = null;
        Set<com.sun.xacml.ctx.Attribute> action = null;
        Set<com.sun.xacml.ctx.Attribute> environment = new HashSet<com.sun.xacml.ctx.Attribute>();

        for (ListIterator<Entity> en = entities.listIterator(); en.hasNext();) {
            Entity entity = en.next();
            String entityType = entity.getEntityType();

            if (entityType.equalsIgnoreCase(AuthorizationConstants.SUBJECT)) {
                subjects = prepareSubject(entity);
            }

            if (entityType.equalsIgnoreCase(AuthorizationConstants.RESOURCE)) {
                resource = prepareResource(entity);

            }

            if (entityType.equalsIgnoreCase(AuthorizationConstants.ACTION)) {
                action = prepareAction(entity);
            }
        }

        RequestCtx xacmlRequest = new RequestCtx(subjects, resource, action,
            environment);

        authorizationRequest = new AuthorizationRequest(xacmlRequest);

        return authorizationRequest;

    }

    public boolean doAuthorization(AuthorizationRequest authorizationRequest)
        throws ForbiddenException {

        if (authorizationRequest == null || pdp == null) {
            if (pdp == null) {
                logger
                    .debug("================================================");
                System.out.println("(SunAuthorizationService) PDP is null");
                logger.debug("(SunAuthorizationService) PDP is null");
                logger
                    .debug("================================================");
            } else {
                System.out
                    .println("(SunAuthorizationService) AuthorizationRequest is null");
            }
            return false;
        }

        RequestCtx request = (RequestCtx) authorizationRequest.getRequest();
        if (request != null) {
            writeRequestToFile(request);

            logger.debug("=== Before doing Authorization ==="
                + request.toString());

            ResponseCtx response = pdp.evaluate(request);

            logger.debug("=== Authorization done ===" + response.toString());
            System.out.println("(== Sun Authorization Service Response ==)"
                + response.toString());
            writeResponseToFile(response);

            Set<Result> resultSet = response.getResults();

            for (Iterator<Result> it = resultSet.iterator(); it.hasNext();) {
                Result result = it.next();

                if (result.getDecision() == Result.DECISION_PERMIT) {
                    logger.debug(" ===== Request permitted. =====");
                    return true;
                }

                if (result.getDecision() == Result.DECISION_DENY
                    || result.getDecision() == Result.DECISION_INDETERMINATE
                    || result.getDecision() == Result.DECISION_NOT_APPLICABLE) {
                    logger.debug("===== Request denied.===== ");

                    String errorMsg = String.format("Authorization failed:");
                    logger.error(errorMsg);
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * Private helper that configures the pdp and the factories based on the
     * settings in the run-time configuration file.
     */
    private void configurePDP(String configFilePath, String policyConfigPath,
        boolean testMode) {

        File configFile = null;
        if (!testMode) {
            URL configFilePathURL = getClass().getResource(configFilePath);
            try {
                configFile = new File(configFilePathURL.toURI());
            } catch (URISyntaxException e) {
                e.printStackTrace();
                throw new IllegalStateException(e);
            }
        } else {
            configFile = new File(configFilePath);
            System.out.println("Looking for config.xml in "
                + configFile.getAbsolutePath());
        }

        ConfigurationStore cs;
        try {
            cs = new ConfigurationStore(configFile);
        } catch (ParsingException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }

        cs.useDefaultFactories();

        policyModule = new FilePolicyFinderModule();

        try {
            loadPolicies(policyConfigPath, testMode);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }

        // use the default factories from the configuration
        cs.useDefaultFactories();

        // get the PDP configuration's policy finder modules...
        PDPConfig config;
        try {
            config = cs.getDefaultPDPConfig();
        } catch (UnknownIdentifierException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
        PolicyFinder finder = config.getPolicyFinder();
        Set<PolicyFinderModule> policyModules = finder.getModules();

        // ...and add the module used by the tests
        policyModules.add(policyModule);
        finder.setModules(policyModules);

        // finally, setup the PDP
        pdp = new PDP(config);
    }

    private Set<Attribute> checkAndAddResourceIdAttributeIfNotPresent(
        Set<Attribute> resourceAttributes) {

        boolean resourceIdAttributePresent = false;
        for (Iterator<Attribute> it = resourceAttributes.iterator(); it
            .hasNext();) {
            Attribute attr = it.next();
            String attributeId = attr.getId().toString();
            if (attributeId
                .equalsIgnoreCase(AuthorizationConstants.RESOURCE_ID_ATTRIBUTE)) {
                resourceIdAttributePresent = true;
                break;
            }
        }

        if (!resourceIdAttributePresent) {

            String issuer = null;
            DateTimeAttribute dateTime = null;

            StringAttribute attributeValue = new StringAttribute(
                "dummyResource");

            try {
                Attribute resourceIdAttribute = new com.sun.xacml.ctx.Attribute(
                    new URI(AuthorizationConstants.RESOURCE_ID_ATTRIBUTE),
                    issuer, dateTime, attributeValue);

                resourceAttributes.add(resourceIdAttribute);
            } catch (Exception exp) {
                exp.printStackTrace();
            }
        }

        return resourceAttributes;
    }

    private List<Attribute> createStringAttributes(
        com.rackspace.idm.authorizationService.AuthorizationAttribute attribute) {
        List<Attribute> attributeValues = new Vector<Attribute>();

        String attributeId = attribute.getId();

        for (Enumeration<String> en = attribute.values(); en.hasMoreElements();) {
            String value = (String) en.nextElement();
            // 1. create 1 or more attributes
            String issuer = null;
            DateTimeAttribute dateTime = null; // new DateTimeAttribute();

            StringAttribute attributeValue = new StringAttribute(value);

            try {
                Attribute subjectAttribute = new com.sun.xacml.ctx.Attribute(
                    new URI(attributeId), issuer, dateTime, attributeValue);
                attributeValues.add(subjectAttribute);
            } catch (URISyntaxException e) {
                logger.error("URI syntax incorrect in creating an attribute.");
            }
        }
        return attributeValues;
    }

    private void loadPolicies(String policyConfigPath, boolean testMode)
        throws Exception {

        HashSet<String> applicablePolicySet = new HashSet<String>();

        applicablePolicySet.add(policyConfigPath);

        policyModule.setPolicies(applicablePolicySet, testMode);
    }

    private Set<Attribute> prepareAction(Entity entity) {

        Set<Attribute> actionAttributes = new HashSet<Attribute>();

        AttributeTable attributeTable = entity.getAttributes();

        actionAttributes = prepareAttributes(attributeTable);

        return actionAttributes;
    }

    private Set<Attribute> prepareAttributes(AttributeTable attributeTable) {
        Set<Attribute> attributes = new HashSet<Attribute>();

        for (Iterator<String> it = attributeTable.keys().iterator(); it
            .hasNext();) {
            String attributeId = it.next();
            com.rackspace.idm.authorizationService.AuthorizationAttribute attribute = (com.rackspace.idm.authorizationService.AuthorizationAttribute) attributeTable
                .get(attributeId);

            String attributeType = attribute.getType();

            if (attributeType
                .equalsIgnoreCase(AuthorizationConstants.TYPE_STRING)) {
                List<Attribute> attrValuesToAdd = createStringAttributes(attribute);
                attributes.addAll(attrValuesToAdd);
            }
        }
        return attributes;
    }

    private Set<Attribute> prepareResource(Entity entity) {

        Set<Attribute> resourceAttributes = new HashSet<Attribute>();

        AttributeTable attributeTable = entity.getAttributes();

        resourceAttributes = prepareAttributes(attributeTable);

        // Sun PDP requires that the request contain the "resource-id"
        // attribute.
        // Add this attribute only if its not already present.

        resourceAttributes = checkAndAddResourceIdAttributeIfNotPresent(resourceAttributes);

        return resourceAttributes;
    }

    private Set<Subject> prepareSubject(Entity entity) {

        Set<Attribute> subjectAttributes = new HashSet<Attribute>();

        AttributeTable attributeTable = entity.getAttributes();

        subjectAttributes = prepareAttributes(attributeTable);

        Subject subject = new Subject(subjectAttributes);

        Set<Subject> subjectSet = new HashSet<Subject>();

        subjectSet.add(subject);

        return subjectSet;
    }

    private void writeRequestToFile(RequestCtx request) {
        request.encode(logOutputStream, new Indenter());
        logger.debug(logOutputStream.toString());
    }

    private void writeResponseToFile(ResponseCtx response) {
        response.encode(logOutputStream, new Indenter());
        logger.debug(logOutputStream.toString());
    }
}
