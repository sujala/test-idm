package com.rackspace.idm.api.converter.cloudv11;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import com.rackspace.idm.domain.entity.User;

public class UserConverterCloudV11 {
    
    private final com.rackspace.idm.cloudv11.jaxb.ObjectFactory OBJ_FACTORY = new com.rackspace.idm.cloudv11.jaxb.ObjectFactory();

    public com.rackspace.idm.cloudv11.jaxb.User toCloudV11User(User user) {
        
        com.rackspace.idm.cloudv11.jaxb.User jaxbUser = OBJ_FACTORY.createUser();
        jaxbUser.setId(user.getUsername());
        jaxbUser.setKey(user.getApiKey());
        jaxbUser.setMossoId(user.getMossoId());
        jaxbUser.setNastId(user.getNastId());
        jaxbUser.setEnabled(!user.isLocked());
        
        try {
            if (user.getCreated() != null) {

                jaxbUser.setCreated(DatatypeFactory.newInstance()
                    .newXMLGregorianCalendar(
                        user.getCreated().toGregorianCalendar()));
            }

            if (user.getUpdated() != null) {
                jaxbUser.setUpdated(DatatypeFactory.newInstance()
                    .newXMLGregorianCalendar(
                        user.getUpdated().toGregorianCalendar()));
            }

        } catch (DatatypeConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        
        return jaxbUser;
    }
    
    public com.rackspace.idm.cloudv11.jaxb.UserWithOnlyEnabled toCloudV11UserWithOnlyEnabled(User user) {
        
        com.rackspace.idm.cloudv11.jaxb.UserWithOnlyEnabled jaxbUser = OBJ_FACTORY.createUserWithOnlyEnabled();
        jaxbUser.setId(user.getUsername());
        jaxbUser.setEnabled(!user.isLocked());
        
        return jaxbUser;
    }
    
    public com.rackspace.idm.cloudv11.jaxb.UserWithId toCloudV11UserWithId(User user) {
        
        com.rackspace.idm.cloudv11.jaxb.UserWithId jaxbUser = OBJ_FACTORY.createUserWithId();
        jaxbUser.setId(user.getUsername());
        
        return jaxbUser;
    }
    
    public com.rackspace.idm.cloudv11.jaxb.UserWithOnlyKey toCloudV11UserWithOnlyKey(User user) {
        
        com.rackspace.idm.cloudv11.jaxb.UserWithOnlyKey jaxbUser = OBJ_FACTORY.createUserWithOnlyKey();
        jaxbUser.setKey(user.getApiKey());
        return jaxbUser;
    }
}
