package com.rackspace.idm.domain.entity;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTimeZone;

import java.util.Locale;

public class UserLocale {
    private Locale preferredLang = null;
    private DateTimeZone timeZone = null;

    /**
     * Attributes are defaulted to en_US, GMT-6:00
     */
    public UserLocale() {

    }

    public UserLocale(Locale preferredLang, DateTimeZone timeZone) {
        this.preferredLang = preferredLang;
        this.timeZone = timeZone;
    }

    public Locale getLocale() {
        return preferredLang;
    }

    /**
     * Discourage the use of the setter outside User
     * 
     * @param preferredLang
     */
    public void setLocale(Locale preferredLang) {
        this.preferredLang = preferredLang;
    }

    public String getPreferredLang() {
        return preferredLang != null ? preferredLang.toString() : null;
    }

    public void setPreferredLang(String lang) {
        preferredLang = parseLocale(lang);
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
                + ((preferredLang == null) ? 0 : preferredLang.hashCode());
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
        if (preferredLang == null) {
            if (other.preferredLang != null) {
                return false;
            }
        } else if (!preferredLang.equals(other.preferredLang)) {
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
        return "UserPreference [preferredLang=" + preferredLang + ", timeZone="
                + timeZone + "]";
    }
}
