package com.rackspace.idm.api.converter;

import org.apache.commons.lang.StringUtils;

import com.rackspace.idm.domain.entity.BaseClient;
import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.ClientStatus;
import com.rackspace.idm.jaxb.Clients;
import com.rackspace.idm.jaxb.ObjectFactory;

public class ClientConverter {

    private PermissionConverter permissionConverter;
    protected ObjectFactory of = new ObjectFactory();

    public ClientConverter(PermissionConverter permissionConverter) {
        this.permissionConverter = permissionConverter;
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

        if (jaxbClient.getPermissions() != null
            && jaxbClient.getPermissions().getPermissions().size() > 0) {
            client.setPermissions(permissionConverter
                .toPermissionListDO(jaxbClient.getPermissions()));
        }

        client.setSoftDeleted(jaxbClient.isSoftDeleted());

        if (jaxbClient.getStatus() != null) {
            client.setStatus(Enum.valueOf(ClientStatus.class, jaxbClient
                .getStatus().toString().toUpperCase()));
        }

        return client;
    }

    public com.rackspace.idm.jaxb.Client toClientJaxbWithoutPermissionsOrCredentials(
        Client client) {
        return toClientJaxb(client, false, false);
    }

    public com.rackspace.idm.jaxb.Client toClientJaxbWithPermissions(
        Client client) {
        return toClientJaxb(client, true, false);
    }

    public com.rackspace.idm.jaxb.Client toClientJaxbWithPermissionsAndCredentials(
        Client client) {
        return toClientJaxb(client, true, true);
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
        boolean includePermissions, boolean includeCredentials) {
        com.rackspace.idm.jaxb.Client returnedClient = of.createClient();

        returnedClient.setClientId(client.getClientId());
        returnedClient.setCustomerId(client.getCustomerId());
        returnedClient.setCustomerInum(client.getOrgInum());
        returnedClient.setIname(client.getIname());
        returnedClient.setInum(client.getInum());
        returnedClient.setLocked(client.isLocked());
        returnedClient.setName(client.getName());
        returnedClient.setSoftDeleted(client.isSoftDeleted());

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

        if (includePermissions && client.getPermissions() != null
            && client.getPermissions().size() > 0) {

            com.rackspace.idm.jaxb.PermissionList perms = permissionConverter
                .toPermissionListJaxb(client.getPermissions());

            returnedClient.setPermissions(perms);
        }

        return returnedClient;
    }

    public com.rackspace.idm.jaxb.Client toClientJaxbFromBaseClient(
        BaseClient client) {
        com.rackspace.idm.jaxb.Client returnedClient = of.createClient();

        returnedClient.setClientId(client.getClientId());
        returnedClient.setCustomerId(client.getCustomerId());

        if (client.getPermissions() != null
            && client.getPermissions().size() > 0) {

            com.rackspace.idm.jaxb.PermissionList perms = permissionConverter
                .toPermissionListJaxb(client.getPermissions());

            returnedClient.setPermissions(perms);
        }

        return returnedClient;
    }
}
