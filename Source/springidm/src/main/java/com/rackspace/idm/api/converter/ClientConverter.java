package com.rackspace.idm.api.converter;

import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.ClientStatus;
import com.rackspace.idm.jaxb.Clients;
import com.rackspace.idm.jaxb.ObjectFactory;

public class ClientConverter {

    private final ObjectFactory of = new ObjectFactory();

    public ClientConverter() {
    }

    public Client toClientDO(com.rackspace.idm.jaxb.Client jaxbClient) {
        Client client = new Client();

        client.setClientId(jaxbClient.getClientId());
        client.setCustomerId(jaxbClient.getCustomerId());

        if (jaxbClient.getCredentials() != null
            && !StringUtils.isBlank(jaxbClient.getCredentials()
                .getClientSecret())) {
            client.setClientSecret(jaxbClient.getCredentials()
                .getClientSecret());
        }

        client.setIname(jaxbClient.getIname());
        client.setInum(jaxbClient.getInum());
        client.setOrgInum(jaxbClient.getCustomerInum());

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

    public com.rackspace.idm.jaxb.Client toClientJaxbWithoutPermissionsOrCredentials(
        Client client) {
        return toClientJaxb(client, false);
    }

    public com.rackspace.idm.jaxb.Client toClientJaxbWithPermissionsAndCredentials(
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

    private com.rackspace.idm.jaxb.Client toClientJaxb(Client client,
        boolean includeCredentials) {
        com.rackspace.idm.jaxb.Client returnedClient = of.createClient();

        returnedClient.setClientId(client.getClientId());
        returnedClient.setCustomerId(client.getCustomerId());
        returnedClient.setCustomerInum(client.getOrgInum());
        returnedClient.setIname(client.getIname());
        returnedClient.setInum(client.getInum());
        returnedClient.setLocked(client.isLocked());
        returnedClient.setName(client.getName());
        returnedClient.setSoftDeleted(client.isSoftDeleted());
        returnedClient.setCallBackUrl(client.getCallBackUrl());
        returnedClient.setTitle(client.getTitle());
        returnedClient.setDescription(client.getDescription());
        returnedClient.setScope(client.getScope());

        if (client.getStatus() != null) {
            returnedClient.setStatus(Enum.valueOf(
                com.rackspace.idm.jaxb.ClientStatus.class, client.getStatus()
                    .toString().toUpperCase()));
        }

        if (includeCredentials && client.getClientSecretObj() != null
            && !StringUtils.isBlank(client.getClientSecret())) {

            com.rackspace.idm.jaxb.ClientCredentials creds = of
                .createClientCredentials();

            creds.setClientSecret(client.getClientSecret());
            returnedClient.setCredentials(creds);
        }

        return returnedClient;
    }

    public com.rackspace.idm.jaxb.Client toClientJaxbFromClient(
        String clientId, String customerId) {
        com.rackspace.idm.jaxb.Client returnedClient = of.createClient();

        returnedClient.setClientId(clientId);
        returnedClient.setCustomerId(customerId);

        return returnedClient;
    }
    
    public com.rackspace.idm.jaxb.Scopes toScopesFromClientList(List<Client> clientList) {
        com.rackspace.idm.jaxb.Scopes scopeAccessList = of.createScopes();
        
        for(Client c : clientList) {
            com.rackspace.idm.jaxb.Scope scopeAccess = of.createScope();
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
