package com.rackspace.idm.api.serviceprofile;

import freemarker.template.Configuration;
import freemarker.template.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.UriInfo;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

@Component
public class ServiceDescriptionTemplateUtil {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
	@Autowired
	private final Configuration freemarkerConfig;
	
	public ServiceDescriptionTemplateUtil() {
		freemarkerConfig = null;
	}
	
	public ServiceDescriptionTemplateUtil(Configuration freemarkerConfig) {
		this.freemarkerConfig = freemarkerConfig;
	}
	
	public String build(final String pattern, final UriInfo uriInfo) {
		try {
			Map<String, String> root = new HashMap<String, String>();
			root.put("baseUrl", uriInfo.getBaseUri().toString());

			Reader in = new StringReader(pattern);
			Writer out = new StringWriter();

			Template template = new Template("template", in, freemarkerConfig);
			template.process(root, out);
			out.flush();

			return out.toString();
		} catch (Exception e) {
			logger.error("Could not load template.", e);
		}

		return "";
	}

    public String build(final String pattern, String uri) {
		try {
			Map<String, String> root = new HashMap<String, String>();
			root.put("baseUrl", uri);

			Reader in = new StringReader(pattern);
			Writer out = new StringWriter();

			Template template = new Template("template", in, freemarkerConfig);
			template.process(root, out);
			out.flush();

			return out.toString();
		} catch (Exception e) {
			logger.error("Could not load template.", e);
		}

		return "";
	}
}
