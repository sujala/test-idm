package com.rackspace.idm.domain.entity;

import junit.framework.Assert;

import org.junit.Test;

import com.rackspace.idm.domain.entity.UserLocale;

import java.util.Locale;

import org.joda.time.DateTimeZone;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class UserLocaleTests {
    private Locale preferredLang = Locale.US;
    private DateTimeZone timeZone = DateTimeZone.forID("America/Chicago");

    private UserLocale getTestLocale() {
        return new UserLocale(preferredLang, timeZone);
    }

    @Test
    public void shouldReturnToString() {
        UserLocale loc = getTestLocale();

        Assert.assertEquals("UserPreference [preferredLang=" + preferredLang
            + ", timeZone=" + timeZone + "]", loc.toString());
    }
    
    @Test
    public void shouldReturnHashCode() {
        UserLocale loc = getTestLocale();
        
        Assert.assertNotNull(loc.hashCode());
        
        loc.setLocale(null);
        loc.setTimeZone(null);
        
        Assert.assertNotNull(loc.hashCode());
    }

    @Test
    public void setPreferredString_null_withNullInput() throws Exception {
        UserLocale locale = getTestLocale();
        locale.setPreferredLang(null);

        assertThat("local prefered lang", locale.getPreferredLang(), nullValue());
    }

    @Test
    public void shouldReturnTrueForEquals() {
        UserLocale loc1 = getTestLocale();
        UserLocale loc2 = getTestLocale();
        
        Assert.assertTrue(loc1.equals(loc1));
        Assert.assertTrue(loc1.equals(loc2));
        
        loc1.setLocale(null);
        loc1.setTimeZone(null);
        
        loc2.setLocale(null);
        loc2.setTimeZone(null);
        
        Assert.assertTrue(loc1.equals(loc2));
    }
    
    @Test
    public void shouldReturnFalseForEquals() {
        UserLocale loc1 = getTestLocale();
        UserLocale loc2 = getTestLocale();
        
        Assert.assertFalse(loc1.equals(null));
        Assert.assertFalse(loc1.equals(1));
        
        loc2.setLocale(Locale.CANADA);
        Assert.assertFalse(loc1.equals(loc2));
        loc2.setLocale(null);
        Assert.assertFalse(loc2.equals(loc1));
        loc2.setLocale(Locale.US);
        
        loc2.setTimeZone(DateTimeZone.forID("America/New_York"));
        Assert.assertFalse(loc1.equals(loc2));
        loc2.setTimeZone(null);
        Assert.assertFalse(loc2.equals(loc1));
    }
}
