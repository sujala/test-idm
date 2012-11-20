package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.entity.FilterParam;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: jacob
 * Date: 11/12/12
 * Time: 4:54 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ClientRoleDao {
    void addClientRole(String clientUniqueId, ClientRole role);
    void updateClientRole(ClientRole role);
    void deleteClientRole(ClientRole role);
    ClientRole getClientRole(ClientRole role);
    List<ClientRole> getClientRoles();
    List<ClientRole> getClientRoles(List<FilterParam> filters);
}
