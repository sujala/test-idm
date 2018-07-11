package com.rackspace.idm.domain.config;

import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;

@Configuration
public class OpenTracingConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(OpenTracingConfiguration.class);

    @Autowired
    public IdentityConfig identityConfig;

    public OpenTracingConfiguration() {
    }

    /**
     * Load the JVM singleton tracer as the "tracer" for the application context.
     * @return
     */
    @Primary
    @Bean(name = "openTracingTracer")
    @Scope
    public Tracer getGlobalTracer(){
        logger.debug("Check if OpenTracing tracer is registered.  We already assume that OpenTracing has been enabled");
        if (!GlobalTracer.isRegistered()) {
            logger.info("GlobalTracer not yet registered");

            try {
                switch (identityConfig.getStaticConfig().getOpenTracingTracer()) {
                    case JAEGER:
                        Tracer tracer = new io.jaegertracing.Configuration(
                                this.identityConfig.getStaticConfig().getOpenTracingServiceName()
                        ).withReporter(getReporterConfiguration()).withSampler(getSamplerConfiguration()).getTracer();

                        GlobalTracer.register(tracer);
                        break;
                    default:
                        logger.error("Invalid tracer specified.  Problem with " +
                                identityConfig.getStaticConfig().getOpenTracingTracer());
                }

            } catch (Exception e) {
                logger.error("Unable to set up tracer: " + e.getMessage(), e);
            }
        }
        return GlobalTracer.get();
    }

    private io.jaegertracing.Configuration.SenderConfiguration getSenderConfiguration() {
        io.jaegertracing.Configuration.SenderConfiguration senderConfiguration = new io.jaegertracing.Configuration.SenderConfiguration();

        if (StringUtils.isNotEmpty(identityConfig.getStaticConfig().getOpenTracingCollectorEndpoint())) {
            senderConfiguration.withEndpoint(identityConfig.getStaticConfig().getOpenTracingCollectorEndpoint());

            // Collector connection.  We can also auth so let's check that
            if (StringUtils.isNotEmpty(identityConfig.getStaticConfig().getOpenTracingCollectorUsername()) &&
                    StringUtils.isNotEmpty(identityConfig.getStaticConfig().getOpenTracingCollectorPassword())) {
                // Username and password defined
                senderConfiguration.withAuthUsername(identityConfig.getStaticConfig().getOpenTracingCollectorUsername());
                senderConfiguration.withAuthPassword(identityConfig.getStaticConfig().getOpenTracingCollectorPassword());
            } else if (StringUtils.isNotEmpty(identityConfig.getStaticConfig().getOpenTracingCollectorToken())) {
                // Token is defined
                senderConfiguration.withAuthToken(identityConfig.getStaticConfig().getOpenTracingCollectorToken());
            }
        } else if (StringUtils.isNotEmpty(identityConfig.getStaticConfig().getOpenTracingAgentHost()) &&
                identityConfig.getStaticConfig().getOpenTracingAgentPort() > 0) {

            // Agent connection.
            senderConfiguration.withAgentHost(identityConfig.getStaticConfig().getOpenTracingAgentHost());
            senderConfiguration.withAgentPort(identityConfig.getStaticConfig().getOpenTracingAgentPort());
        }
        return senderConfiguration;
    }

    private io.jaegertracing.Configuration.SamplerConfiguration getSamplerConfiguration() {
        io.jaegertracing.Configuration.SamplerConfiguration samplerConfiguration = new io.jaegertracing.Configuration.SamplerConfiguration();

        if (identityConfig.getStaticConfig().getOpenTracingConstantToggle() > -1) {
            // Constant sampling set (can either be 0 or 1 so must check if greater than -1)
            samplerConfiguration.withType(OpenTracingSamplingEnum.CONST.getName()).withParam(
                    identityConfig.getStaticConfig().getOpenTracingConstantToggle());
        } else if (identityConfig.getStaticConfig().getOpenTracingProbability() > -1) {
            // Probabilistic sampling is set. Can be 0.0 so must check if greater than -1.
            samplerConfiguration.withType(OpenTracingSamplingEnum.PROBABILISTIC.getName()).withParam(
                    identityConfig.getStaticConfig().getOpenTracingProbability()
            );
        } else if (identityConfig.getStaticConfig().getOpenTracingRateLimitingLimit() > -1) {
            // RateLimiting sampling is set. Can be 0.0 so must check if greater than -1.
            samplerConfiguration.withType(OpenTracingSamplingEnum.RATELIMITING.getName()).withParam(
                    identityConfig.getStaticConfig().getOpenTracingRateLimitingLimit()
            );
        }

        return  samplerConfiguration;
    }

    private io.jaegertracing.Configuration.ReporterConfiguration getReporterConfiguration() {
        return new io.jaegertracing.Configuration.ReporterConfiguration().withFlushInterval(
                identityConfig.getStaticConfig().getOpenTracingFlushIntervalMs()
        ).withLogSpans(
                identityConfig.getStaticConfig().getOpenTracingLoggingEnabled()
        ).withMaxQueueSize(
                identityConfig.getStaticConfig().getOpenTracingMaxBufferSize()
        ).withSender(getSenderConfiguration());
    }
}