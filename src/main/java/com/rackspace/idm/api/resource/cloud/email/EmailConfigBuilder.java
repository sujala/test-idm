package com.rackspace.idm.api.resource.cloud.email;

import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.ApiDocDao;
import com.rackspace.idm.exception.ForbiddenException;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailPreparationException;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.List;

@Component
public class EmailConfigBuilder {

    @Autowired
    private ApiDocDao apiDocDao;

    @Autowired
    private IdentityConfig identityConfig;

    public EmailConfig buildEmailConfig(List<String> recipients, String basePath, String prefix) {
        PropertiesConfiguration extraProps = getExtraEmailProps(basePath, prefix);
        EmailConfig config = new EmailConfig(recipients, basePath, prefix, extraProps);

        //validate can send this email
        if (identityConfig.getReloadableConfig().isSendToOnlyRackspaceAddressesEnabled()) {
            for (String recipient : recipients) {
                if (!recipient.toLowerCase().endsWith(MailTransferAgentClient.RACKSPACE_EMAIL)) {
                    throw new ForbiddenException(MailTransferAgentClient.FORBIDDEN_MESSAGE);
                }
            }
        }

        return config;
    }

    private PropertiesConfiguration getExtraEmailProps(String basePath, String prefix) {
        PropertiesConfiguration config = new PropertiesConfiguration();

        String extraPropsPath = basePath + File.separator + prefix + EmailConfig.PROPERTIES_SUFFIX_NAME;
        String extraPropStr = apiDocDao.getContent(extraPropsPath);
        if (StringUtils.isNotBlank(extraPropStr)) {
            try {
                config.load(new ByteArrayInputStream(extraPropStr.getBytes()));
            } catch (ConfigurationException e) {
                throw new MailPreparationException(String.format("Error reading extra email props at '%s'", extraPropsPath));
            }
        }

        //set a default from if one is not provided
        if (StringUtils.isBlank(config.getString(EmailConfig.EXTRA_PROP_FROM))) {
            config.setProperty(EmailConfig.EXTRA_PROP_FROM, identityConfig.getReloadableConfig().getEmailFromAddress());
        }

        return config;
    }
}
