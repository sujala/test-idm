package com.rackspace.idm.api.resource.cloud.email;

import lombok.Getter;
import lombok.experimental.Delegate;
import org.apache.commons.configuration.PropertiesConfiguration;

import java.io.File;
import java.util.List;

public class EmailConfig {
    public static final String CONTENT_TEMPLATE_SUFFIX_NAME = "_content.vm";
    public static final String CONTENT_HTML_TEMPLATE_SUFFIX_NAME = "_content_html.vm";
    public static final String SUBJECT_TEMPLATE_SUFFIX_NAME = "_subject.vm";
    public static final String PROPERTIES_SUFFIX_NAME = ".properties";

    public static final String EXTRA_PROP_FROM = "from";
    public static final String EXTRA_PROP_REPLY_TO = "reply_to";

    @Getter
    private String subjectVelocityTemplatePath;

    @Getter
    private String contentVelocityTemplatePath;

    @Getter
    private String contentHtmlVelocityTemplatePath;

    @Getter
    private PropertiesConfiguration extraProperties;

    @Getter
    @Delegate
    private EmailMetaProperties emailMetaProperties;

    public EmailConfig(List<String> recipients, String emailPath, String prefix, PropertiesConfiguration extraProperties) {
        this.subjectVelocityTemplatePath = EmailConfig.constructEmailPartPath(emailPath, prefix, SUBJECT_TEMPLATE_SUFFIX_NAME);
        this.contentVelocityTemplatePath = EmailConfig.constructEmailPartPath(emailPath, prefix, CONTENT_TEMPLATE_SUFFIX_NAME);
        this.contentHtmlVelocityTemplatePath = EmailConfig.constructEmailPartPath(emailPath, prefix, CONTENT_HTML_TEMPLATE_SUFFIX_NAME);
        this.extraProperties = extraProperties;

        //extract the standard props and push to in emailMeta
        emailMetaProperties = new EmailMetaProperties(recipients
                , extraProperties.getString(EXTRA_PROP_FROM)
                , extraProperties.getString(EXTRA_PROP_REPLY_TO));
    }

    private static String constructEmailPartPath(String emailPath, String prefix, String suffix) {
        return emailPath + File.separator + prefix + suffix;
    }


}
