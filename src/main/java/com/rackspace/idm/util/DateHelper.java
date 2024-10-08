package com.rackspace.idm.util;

import org.springframework.stereotype.Component;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

@Component
public class DateHelper {

    public BigInteger getSecondsFromDuration(Duration duration) {
        return duration == null ? null : BigInteger.valueOf(duration.getTimeInMillis(new Date(0)) / 1000l);
    }

    public Duration getDurationFromSeconds(Integer seconds) {
        if (seconds == null) {
            return null;
        }
        try {
            final DatatypeFactory factory = DatatypeFactory.newInstance();
            return factory.newDuration(seconds * 1000l);
        } catch (DatatypeConfigurationException ex) {
            throw new RuntimeException(ex);
        }
    }

    public Date addSecondsToDate(Date date, int seconds) {
        final Calendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        calendar.add(Calendar.SECOND, seconds);
        return calendar.getTime();
    }

}
