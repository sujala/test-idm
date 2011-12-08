package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.domain.entity.TenantRole;
import org.openstack.docs.identity.api.v2.User;
import org.openstack.docs.identity.api.v2.UserForAuthenticateResponse;
import org.openstack.docs.identity.api.v2.UserList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.util.List;

@Component
public class UserConverterCloudV20 {
    
    @Autowired
    private JAXBObjectFactories OBJ_FACTORIES;
    
    @Autowired
    private RoleConverterCloudV20 roleConverterCloudV20;
    
    public com.rackspace.idm.domain.entity.User toUserDO(User user) {
        
        com.rackspace.idm.domain.entity.User userDO = new com.rackspace.idm.domain.entity.User();
        userDO.setUsername(user.getUsername());
        userDO.setEmail(user.getEmail());
        userDO.setDisplayName(user.getDisplayName());
        return userDO;
    }

    public UserForAuthenticateResponse toUserForAuthenticateResponse(com.rackspace.idm.domain.entity.User user, List<TenantRole> roles) {
        UserForAuthenticateResponse jaxbUser = OBJ_FACTORIES.getOpenStackIdentityV2Factory().createUserForAuthenticateResponse();
        
        jaxbUser.setId(user.getId());
        jaxbUser.setName(user.getUsername());
        jaxbUser.setRoles(this.roleConverterCloudV20.toRoleListJaxb(roles));
        
        return jaxbUser;
    }
    
    public User toUser(com.rackspace.idm.domain.entity.User user) {
        User jaxbUser = OBJ_FACTORIES.getOpenStackIdentityV2Factory().createUser();
        
        jaxbUser.setDisplayName(user.getDisplayName());
        jaxbUser.setEmail(user.getEmail());
        jaxbUser.setEnabled(user.isEnabled());
        jaxbUser.setId(user.getId());
        jaxbUser.setUsername(user.getUsername());
        
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
    
    public UserList toUserList(List<com.rackspace.idm.domain.entity.User> users) {
        
        UserList list = OBJ_FACTORIES.getOpenStackIdentityV2Factory().createUserList();
        
        for (com.rackspace.idm.domain.entity.User user : users) {
            list.getUser().add(this.toUser(user));
        }
        
        return list;
    }
}
