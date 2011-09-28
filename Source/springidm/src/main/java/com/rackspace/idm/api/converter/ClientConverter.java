package com.rackspace.idm.api.converter;

import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.rackspace.api.idm.v1.Clients;
import com.rackspace.api.idm.v1.ObjectFactory;
import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.ClientStatus;

public class ClientConverter {

    private final ObjectFactory of = new ObjectFactory();

    public ClientConverter() {
    }

    public Client toClientDO(com.rackspace.api.idm.v1.Client jaxbClient) {
        Client client = new Client();

        client.setClientId(jaxbClient.getClientId());
        client.setRCN(jaxbClient.getCustomerId());

        if (jaxbClient.getCredentials() != null
            && !StringUtils.isBlank(jaxbClient.getCredentials()
                .getClientSecret())) {
            client.setClientSecret(jaxbClient.getCredentials()
                .getClientSecret());
        }

        client.setLocked(jaxbClient.isLocked());

        client.setName(jaxbClient.getName());

        client.setSoftDeleted(jaxbClient.isSoftDeleted());
        
        client.setCallBackUrl(jaxbClient.getCallBackUrl());
        client.setTitle(jaxbClient.getTitle());
        client.setDescription(jaxbClient.getDescription());
        client.setScope(jaxbClient.getScope());

        if (jaxbClient.getStatus() != null) {
            client.setStatus(Enum.valueOf(ClientStatus.class, jaxbClient
                .getStatus().toString().toUpperCase()));
        }

        return client;
    }

    public com.rackspace.api.idm.v1.Client toClientJaxbWithoutPermissionsOrCredentials(
        Client client) {
        return toClientJaxb(client, false);
    }

    public com.rackspace.api.idm.v1.Client toClientJaxbWithPermissionsAndCredentials(
        Client client) {
        return toClientJaxb(client, true);
    }

    public Clients toClientListJaxb(com.rackspace.idm.domain.entity.Clients clients) {

        if (clients == null || clients.getClients().size() < 1) {
            return null;
        }

        Clients returnedClients = of.createClients();

        for (Client client : clients.getClients()) {
            returnedClients.getClients().add(
                toClientJaxbWithoutPermissionsOrCredentials(client));
        }
        
        returnedClients.setLimit(clients.getLimit());
        returnedClients.setOffset(clients.getOffset());
        returnedClients.setTotalRecords(clients.getTotalRecords());

        return returnedClients;
    }

    private com.rackspace.api.idm.v1.Client toClientJaxb(Client client,
        boolean includeCredentials) {
        com.rackspace.api.idm.v1.Client returnedClient = of.createClient();

        returnedClient.setClientId(client.getClientId());
        returnedClient.setCustomerId(client.getRCN());
        returnedClient.setLocked(client.isLocked());
        returnedClient.setName(client.getName());
        returnedClient.setSoftDeleted(client.isSoftDeleted());
        returnedClient.setCallBackUrl(client.getCallBackUrl());
        returnedClient.setTitle(client.getTitle());
        returnedClient.setDescription(client.getDescription());
        returnedClient.setScope(client.getScope());

        if (client.getStatus() != null) {
            returnedClient.setStatus(Enum.valueOf(
                com.rackspace.api.idm.v1.ClientStatus.class, client.getStatus()
                    .toString().toUpperCase()));
        }

        if (includeCredentials && client.getClientSecretObj() != null
            && !StringUtils.isBlank(client.getClientSecret())) {

            com.rackspace.api.idm.v1.ClientCredentials creds = of
                .createClientCredentials();

            creds.setClientSecret(client.getClientSecret());
            returnedClient.setCredentials(creds);
        }

        return returnedClient;
    }

    public com.rackspace.api.idm.v1.Client toClientJaxbFromClient(
        String clientId, String customerId) {
        com.rackspace.api.idm.v1.Client returnedClient = of.createClient();

        returnedClient.setClientId(clientId);
        returnedClient.setCustomerId(customerId);

        return returnedClient;
    }
    
    public com.rackspace.api.idm.v1.Scopes toScopesFromClientList(List<Client> clientList) {
        com.rackspace.api.idm.v1.Scopes scopeAccessList = of.createScopes();
        
        for(Client c : clientList) {
            com.rackspace.api.idm.v1.Scope scopeAccess = of.createScope();
            if ( c.getScope() != null) {
                scopeAccess.setName(c.getScope());
            }
            if (c.getCallBackUrl() != null) {
                scopeAccess.setUrl(c.getCallBackUrl());
            }
            scopeAccessList.getScopes().add(scopeAccess);
        }
        return scopeAccessList;
    }
}
