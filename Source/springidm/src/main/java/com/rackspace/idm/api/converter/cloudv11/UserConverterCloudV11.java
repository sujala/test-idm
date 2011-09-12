package com.rackspace.idm.api.converter.cloudv11;

import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import com.rackspace.idm.domain.entity.CloudEndpoint;
import com.rackspace.idm.domain.entity.User;

public class UserConverterCloudV11 {
    
    private final EndpointConverterCloudV11 enpointConverterCloudV11;
    
    private final com.rackspacecloud.docs.auth.api.v1.ObjectFactory OBJ_FACTORY = new com.rackspacecloud.docs.auth.api.v1.ObjectFactory();

    public UserConverterCloudV11(EndpointConverterCloudV11 enpointConverterCloudV11) {
        this.enpointConverterCloudV11 = enpointConverterCloudV11;
    }
    
    public com.rackspacecloud.docs.auth.api.v1.User toCloudV11User(User user, List<CloudEndpoint> endpoints) {
        
        com.rackspacecloud.docs.auth.api.v1.User jaxbUser = OBJ_FACTORY.createUser();
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
        
        if (endpoints != null && endpoints.size() > 0) {
            jaxbUser.setBaseURLRefs(this.enpointConverterCloudV11.toBaseUrlRefs(endpoints));
        }
        
        return jaxbUser;
    }
    
    public com.rackspacecloud.docs.auth.api.v1.UserWithOnlyEnabled toCloudV11UserWithOnlyEnabled(User user) {
        
        com.rackspacecloud.docs.auth.api.v1.UserWithOnlyEnabled jaxbUser = OBJ_FACTORY.createUserWithOnlyEnabled();
        jaxbUser.setId(user.getUsername());
        jaxbUser.setEnabled(!user.isLocked());
        
        return jaxbUser;
    }
    
    public com.rackspacecloud.docs.auth.api.v1.UserWithId toCloudV11UserWithId(User user) {
        
        com.rackspacecloud.docs.auth.api.v1.UserWithId jaxbUser = OBJ_FACTORY.createUserWithId();
        jaxbUser.setId(user.getUsername());
        
        return jaxbUser;
    }
    
    public com.rackspacecloud.docs.auth.api.v1.UserWithOnlyKey toCloudV11UserWithOnlyKey(User user) {
        
        com.rackspacecloud.docs.auth.api.v1.UserWithOnlyKey jaxbUser = OBJ_FACTORY.createUserWithOnlyKey();
        jaxbUser.setKey(user.getApiKey());
        return jaxbUser;
    }
}
