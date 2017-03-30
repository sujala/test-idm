package com.rackspace.idm.domain.dozer.converters;

import org.dozer.DozerConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

public class DurationStringConverter extends
        DozerConverter<String, Duration> {

    private Logger logger = LoggerFactory.getLogger(DurationStringConverter.class);

    public DurationStringConverter() {
        super(String.class, Duration.class);
    }

    @Override
    public String convertFrom(Duration src, String dest) {
        if (src != null) {
            return src.toString();
        }
        return null;
    }

    @Override
    public Duration convertTo(String src, Duration dest) {
        if (src != null) {
            try {
                return DatatypeFactory.newInstance().newDuration(src);
            } catch (DatatypeConfigurationException e) {
                logger.error("Failed to create Duration: " + e.getMessage());
            }
        }
        return null;
    }
}