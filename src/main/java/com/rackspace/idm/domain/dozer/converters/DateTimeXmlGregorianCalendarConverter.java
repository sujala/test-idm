package com.rackspace.idm.domain.dozer.converters;

import org.dozer.DozerConverter;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.GregorianCalendar;

/**
 * Special converter to help with gregorian calendar objects.
 * There is a bug with xerces. open saml has xerces as a dependency,
 * so this is what causes the problem, and why we need this class.
 * http://sourceforge.net/p/dozer/bugs/313/
 */
public class DateTimeXmlGregorianCalendarConverter extends
        DozerConverter<DateTime, XMLGregorianCalendar> {

    private Logger logger = LoggerFactory.getLogger(DateTimeXmlGregorianCalendarConverter.class);

    public DateTimeXmlGregorianCalendarConverter() {
        super(DateTime.class, XMLGregorianCalendar.class);
    }

    @Override
    public DateTime convertFrom(XMLGregorianCalendar src, DateTime dest) {
        if (src != null) {
            return new DateTime(src.toGregorianCalendar().getTime().getTime());
        }

        return null;
    }

    @Override
    public XMLGregorianCalendar convertTo(DateTime src, XMLGregorianCalendar dest) {
        try {
            if (src != null) {
                GregorianCalendar gc = new GregorianCalendar();
                gc.setTime(src.toDate());
                return DatatypeFactory.newInstance().newXMLGregorianCalendar(gc);
            }
        } catch (DatatypeConfigurationException e) {
            logger.error("failed to create XMLGregorianCalendar: " + e.getMessage());
        }

        return null;
    }

}