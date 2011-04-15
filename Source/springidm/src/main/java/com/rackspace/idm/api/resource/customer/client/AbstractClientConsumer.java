package com.rackspace.idm.api.resource.customer.client;

import org.slf4j.Logger;

import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.service.ClientService;
import com.rackspace.idm.exception.NotFoundException;

public abstract class AbstractClientConsumer {
    private ClientService clientService;

    protected AbstractClientConsumer(ClientService clientService) {
        this.clientService = clientService;
    }

    protected Client checkAndGetClient(String customerId, String clientId) throws NotFoundException {
        Client client = this.clientService.getById(clientId);

        if (client == null || !client.getCustomerId().toLowerCase().equals(customerId.toLowerCase())) {
            String errorMsg = String.format("Client Not Found: %s", clientId);
            getLogger().warn(errorMsg);
            throw new NotFoundException(errorMsg);
        }
        return client;
    }

    protected abstract Logger getLogger();
}
