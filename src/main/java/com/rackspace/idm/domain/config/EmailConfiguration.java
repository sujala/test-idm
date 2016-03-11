package com.rackspace.idm.domain.config;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.tools.ToolContext;
import org.apache.velocity.tools.ToolManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class EmailConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(EmailConfiguration.class);

    @Autowired
    public IdentityConfig identityConfig;

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailer = new JavaMailSenderImpl();
        mailer.setHost(identityConfig.getStaticConfig().getEmailHost());
        mailer.setPort(identityConfig.getStaticConfig().getEmailPort());
        mailer.setUsername(identityConfig.getStaticConfig().getEmailUsername());
        mailer.setPassword(identityConfig.getStaticConfig().getEmailPassword());
        Properties props = new Properties();
        props.put("mail.smtp.timeout", 60000);
        props.put("smtp.connectiontimeout", 60000);
        mailer.setJavaMailProperties(props);

        return mailer;
    }

    @Bean
    public ToolManager toolManager() {
        return new ToolManager();
    }

    @Bean
    public ToolContext emailVelocityToolContext() {
        return toolManager().createContext();
    }

    @Bean
    public VelocityEngine emailVelocityEngine() {
        Properties props = new Properties();
        props.put("resource.loader", "file, class");
        props.put("file.resource.loader.description", "Velocity File Resource Loader");
        props.put("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.FileResourceLoader");
        props.put("file.resource.loader.path", identityConfig.getConfigRoot());
        props.put("file.resource.loader.modificationCheckInterval", "30");
        props.put("class.resource.loader.description", "Velocity Classpath Resource Loader");
        props.put("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        props.put(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS, "org.apache.velocity.runtime.log.Log4JLogChute");
        props.put("runtime.log.logsystem.log4j.logger", "VelocityEngine");

        return new VelocityEngine(props);
    }
}
