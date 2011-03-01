package com.rackspace.idm.entities;

import junit.framework.Assert;

import org.junit.Test;

import com.rackspace.idm.domain.entity.UserHumanName;

public class UserHumanNameTests {

    private String firstname = "First";
    private String middlename = "Midlle";
    private String lastname = "Last";

    private UserHumanName getTestHumanName() {
        return new UserHumanName(firstname, middlename, lastname);
    }

    @Test
    public void shouldReturnToString() {
        UserHumanName name = getTestHumanName();
        Assert.assertEquals("UserHumanName [firstname=" + firstname
            + ", middlename=" + middlename + ", lastname=" + lastname + "]",
            name.toString());
    }
    
    @Test
    public void shouldReturnHashCode() {
        UserHumanName name = getTestHumanName();
        Assert.assertEquals(-1101617482, name.hashCode());
        
        name.setFirstname(null);
        name.setMiddlename(null);
        name.setLastname(null);
        
        Assert.assertEquals(29791, name.hashCode());
    }
    
    @Test
    public void shouldReturnTrueForEquals() {
        UserHumanName name1 = getTestHumanName();
        UserHumanName name2 = getTestHumanName();
        
        Assert.assertTrue(name1.equals(name1));
        Assert.assertTrue(name1.equals(name2));
        
        name1.setFirstname(null);
        name1.setLastname(null);
        name1.setMiddlename(null);
        
        name2.setFirstname(null);
        name2.setLastname(null);
        name2.setMiddlename(null);
        
        Assert.assertTrue(name1.equals(name2));
    }
    
    @Test
    public void shouldReturnFalseForEquals() {
        UserHumanName name1 = getTestHumanName();
        UserHumanName name2 = getTestHumanName();
        
        Assert.assertFalse(name1.equals(null));
        Assert.assertFalse(name1.equals(1));
        
        name2.setFirstname("NewFirst");
        Assert.assertFalse(name1.equals(name2));
        name2.setFirstname(null);
        Assert.assertFalse(name2.equals(name1));
        name2.setFirstname(name1.getFirstname());
        
        name2.setMiddlename("NewMiddle");
        Assert.assertFalse(name1.equals(name2));
        name2.setMiddlename(null);
        Assert.assertFalse(name2.equals(name1));
        name2.setMiddlename(name1.getMiddlename());
        
        name2.setLastname("NewLast");
        Assert.assertFalse(name1.equals(name2));
        name2.setLastname(null);
        Assert.assertFalse(name2.equals(name1));
    }
}
