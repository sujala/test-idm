package com.rackspace.idm.domain.entity;

import java.util.Locale;

import javax.xml.bind.annotation.XmlTransient;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTimeZone;

public class UserLocale {
    private Locale prefferedLang = null;
    private DateTimeZone timeZone = null;

    /**
     * Attributes are defaulted to en_US, GMT-6:00
     */
    public UserLocale() {

    }

    public UserLocale(Locale prefferedLang, DateTimeZone timeZone) {
        this.prefferedLang = prefferedLang;
        this.timeZone = timeZone;
    }

    @XmlTransient
    public Locale getLocale() {
        return prefferedLang;
    }

    /**
     * Discourage the use of the setter outside User
     * 
     * @param prefferedLang
     */
    public void setLocale(Locale prefferedLang) {
        this.prefferedLang = prefferedLang;
    }

    public String getPrefferedLang() {
        return prefferedLang != null ? prefferedLang.toString() : null;
    }

    public void setPrefferedLang(String lang) {
        prefferedLang = parseLocale(lang);
    }

    private Locale parseLocale(String lang) {
        String[] localeParams = StringUtils.split(lang, '_');
        Locale userLocale = null;
        if (localeParams != null) {
            if (localeParams.length == 2) {
                userLocale = new Locale(localeParams[0], localeParams[1]
                        .toUpperCase());
            } else {
                userLocale = new Locale(localeParams[0]);
            }
        }
        return userLocale;
    }

    public DateTimeZone getTimeZone() {
        return timeZone;
    }

    /**
     * Discourage the use of the setter outside User
     * 
     * @param timeZone
     */
    public void setTimeZone(DateTimeZone timeZone) {
        this.timeZone = timeZone;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((prefferedLang == null) ? 0 : prefferedLang.hashCode());
        result = prime * result
                + ((timeZone == null) ? 0 : timeZone.getID().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        UserLocale other = (UserLocale) obj;
        if (prefferedLang == null) {
            if (other.prefferedLang != null) {
                return false;
            }
        } else if (!prefferedLang.equals(other.prefferedLang)) {
            return false;
        }
        if (timeZone == null) {
            if (other.timeZone != null) {
                return false;
            }
        } else if (!timeZone.getID().equals(other.timeZone.getID())) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "UserPreference [prefferedLang=" + prefferedLang + ", timeZone="
                + timeZone + "]";
    }
}
