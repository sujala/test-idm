package com.rackspace.idm.api.resource.cloud.email;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.tools.ToolContext;
import org.apache.velocity.tools.ToolManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

@Component
public class VelocityUtils {
    private static final Log LOGGER = LogFactory.getLog(VelocityUtils.class);

    private ToolContext toolContext = null;
    private VelocityEngine velocityEngine = null;

    public static final String DEFAULT_ENCODING = "UTF8";
    private String defaultEncoding = DEFAULT_ENCODING;

    @Autowired
    public VelocityUtils(VelocityEngine velocityEngine, ToolContext toolContext) {
        this.toolContext = toolContext;
        this.velocityEngine = velocityEngine;
    }

    public VelocityUtils() {
        ToolManager toolManager = new ToolManager();
        toolContext = toolManager.createContext();
        velocityEngine = new VelocityEngine();
    }

    public Context createContext(Map model) {
        return new VelocityContext(model, toolContext);
    }

    /**
     * Merge the specified Velocity template with the given model using default encoding and write
     * @param templateLocation the location of template, relative to Velocity's
     * resource loader path
     * @param model the Map that contains model names as keys and model objects
     * as values
     * @param writer the Writer to write the result to
     * @throws org.apache.velocity.exception.VelocityException if the template wasn't found or rendering failed
     */
    public void mergeTemplate(String templateLocation, Map model, Writer writer)
            throws VelocityException {
        mergeTemplate(templateLocation, defaultEncoding, model, writer);
    }

    /**
     * Merge the specified Velocity template with the given model and write
     * the result to the given Writer.
     * @param templateLocation the location of template, relative to Velocity's
     * resource loader path
     * @param encoding the encoding of the template file
     * @param model the Map that contains model names as keys and model objects
     * as values
     * @param writer the Writer to write the result to
     * @throws VelocityException if the template wasn't found or rendering failed
     */
    public void mergeTemplate(String templateLocation, String encoding, Map model, Writer writer)
            throws VelocityException {

        try {
            Context velocityContext = createContext(model);
            velocityEngine.mergeTemplate(templateLocation, encoding, velocityContext, writer);
        }
        catch (VelocityException ex) {
            throw ex;
        }
        catch (RuntimeException ex) {
            throw ex;
        }
        catch (Exception ex) {
            LOGGER.error("Why does VelocityEngine throw a generic checked exception, after all?", ex);
            throw new VelocityException(ex.toString());
        }
    }

    /**
     * Merge the specified Velocity template with the given model into a String using default encoding.
     * <p>When using this method to prepare a text for a mail to be sent with Spring's
     * mail support, consider wrapping VelocityException in MailPreparationException.
     * @param templateLocation the location of template, relative to Velocity's
     * resource loader path
     * @param model the Map that contains model names as keys and model objects
     * as values
     * @return the result as String
     * @throws VelocityException if the template wasn't found or rendering failed
     * @see org.springframework.mail.MailPreparationException
     */
    public String mergeTemplateIntoString(String templateLocation, Map model)
            throws VelocityException {

        StringWriter result = new StringWriter();
        mergeTemplate(templateLocation, model, result);
        return result.toString();
    }

    /**
     * Merge the specified Velocity template with the given model into a String.
     * <p>When using this method to prepare a text for a mail to be sent with Spring's
     * mail support, consider wrapping VelocityException in MailPreparationException.
     * @param templateLocation the location of template, relative to Velocity's
     * resource loader path
     * @param encoding the encoding of the template file
     * @param model the Map that contains model names as keys and model objects
     * as values
     * @return the result as String
     * @throws VelocityException if the template wasn't found or rendering failed
     * @see org.springframework.mail.MailPreparationException
     */
    public String mergeTemplateIntoString(String templateLocation, String encoding, Map model)
            throws VelocityException {

        StringWriter result = new StringWriter();
        mergeTemplate(templateLocation, encoding, model, result);
        return result.toString();
    }

    public String getDefaultEncoding() {
        return defaultEncoding;
    }

    public void setDefaultEncoding(String defaultEncoding) {
        this.defaultEncoding = defaultEncoding;
    }
}
