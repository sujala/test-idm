package com.rackspace.idm.api.resource.cloud.v20.json.readers;

import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.exception.IdmException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openstack.docs.identity.api.v2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class JSONReaderForCloudAuthenticationResponseServiceCatalog {

    private static final Logger LOGGER = LoggerFactory.getLogger(JSONReaderForCloudAuthenticationResponseServiceCatalog.class);

    public static ServiceCatalog parse(String jsonBody) {
        ServiceCatalog serviceCatalog = new ServiceCatalog();

        try {
            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);

            if(outer.containsKey(JSONConstants.ACCESS)) {
                JSONObject accessJson = (JSONObject) parser.parse(outer.get(JSONConstants.ACCESS).toString());

                if(accessJson.containsKey(JSONConstants.SERVICECATALOG)) {
                    JSONArray catalogEntries = (JSONArray) accessJson.get(JSONConstants.SERVICECATALOG);
                    for(Object catalogEntryObject : catalogEntries) {
                        JSONObject catalogEntry = (JSONObject) catalogEntryObject;
                        ServiceForCatalog serviceForCatalog = new ServiceForCatalog();

                        if(catalogEntry.containsKey(JSONConstants.NAME)) {
                            serviceForCatalog.setName((String) catalogEntry.get(JSONConstants.NAME));
                        }

                        if(catalogEntry.containsKey(JSONConstants.TYPE)) {
                            serviceForCatalog.setType((String) catalogEntry.get(JSONConstants.TYPE));
                        }

                        if(catalogEntry.containsKey(JSONConstants.ENDPOINTS)) {
                            JSONArray endpointEntries = (JSONArray) catalogEntry.get(JSONConstants.ENDPOINTS);

                            for(Object endpointEntryObject : endpointEntries) {
                                JSONObject endpointEntry = (JSONObject) endpointEntryObject;
                                EndpointForService endpointForService = new EndpointForService();

                                if(endpointEntry.containsKey(JSONConstants.REGION)) {
                                    endpointForService.setRegion((String) endpointEntry.get(JSONConstants.REGION));
                                }

                                if(endpointEntry.containsKey(JSONConstants.TENANT_ID)) {
                                    endpointForService.setTenantId((String) endpointEntry.get(JSONConstants.TENANT_ID));
                                }

                                if(endpointEntry.containsKey(JSONConstants.PUBLIC_URL)) {
                                    endpointForService.setPublicURL((String) endpointEntry.get(JSONConstants.PUBLIC_URL));
                                }

                                if(endpointEntry.containsKey(JSONConstants.INTERNAL_URL)) {
                                    endpointForService.setInternalURL((String) endpointEntry.get(JSONConstants.INTERNAL_URL));
                                }

                                if(endpointEntry.containsKey(JSONConstants.ADMIN_URL)) {
                                    endpointForService.setAdminURL((String) endpointEntry.get(JSONConstants.ADMIN_URL));
                                }

                                serviceForCatalog.getEndpoint().add(endpointForService);
                            }

                        }

                        serviceCatalog.getService().add(serviceForCatalog);
                    }
                }

            }

        } catch (ParseException e) {
            LOGGER.info(e.toString());
            throw new IdmException("unable to parse Cloud AuthenticationResponse serviceCatalog", e);
        }
        return serviceCatalog;
    }
}
