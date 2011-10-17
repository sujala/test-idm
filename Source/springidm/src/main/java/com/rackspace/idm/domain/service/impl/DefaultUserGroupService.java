package com.rackspace.idm.domain.service.impl;

import com.rackspace.cloud.servers.bean.LimitGroupType;
import com.rackspace.cloud.service.servers.CloudServers;
import com.rackspace.cloud.service.servers.CloudServersFault;
import com.rackspace.cloud.service.servers.UnauthorizedFault;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group;
import com.rackspace.idm.domain.entity.ESBCloudServersFactory;
import com.rackspace.idm.domain.service.UserGroupService;
import com.rackspace.idm.exception.ApiException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 10/12/11
 * Time: 5:26 PM
 */
@Component
public class DefaultUserGroupService implements UserGroupService {

    private static final Logger LOGGER = Logger.getLogger(UserGroupService.class);

    @Autowired
    private ESBCloudServersFactory esbCloudServersFactory;

    private CloudServers csClient;

    @Override
    public List<Group> getGroups(Integer mossoAccountId) {
        try {
            List<Group> groups = null;
            csClient = esbCloudServersFactory.getCSClient(String.valueOf(mossoAccountId));
            LimitGroupType limitGroupType = csClient.getAPILimitsForAccount(mossoAccountId);
            if (limitGroupType != null) {
                groups = convertGroup(limitGroupType);
            }
            return groups;
        } catch (CloudServersFault cloudServersFault) {
            System.out.println(cloudServersFault);
            LOGGER.error("Unable to create client to Cloud Servers ESB service.");
            throw new ApiException(500,"An error was encountered while trying to connect to Cloud Servers.", "");
        } catch (UnauthorizedFault unauthorizedFault) {
            LOGGER.error("Unable to create client to Cloud Servers ESB service.");
            throw new ApiException(500,"An error was encountered while trying to connect to Cloud Servers.", "");
        }
    }

    private List<Group> convertGroup(LimitGroupType limitGroupType) {

        List<Group> groups = new ArrayList<Group>();
        Group group = new Group();

        group.setId(limitGroupType.getName());

        final String groupDescription = limitGroupType.getDescription();
        if (StringUtils.isNotEmpty(groupDescription)) {
            group.setDescription(limitGroupType.getDescription());
        }

        groups.add(group);
        return groups;
    }

    public void setEsbCloudServersFactory(ESBCloudServersFactory esbCloudServersFactory) {
        this.esbCloudServersFactory = esbCloudServersFactory;
    }

    public void setCsClient(CloudServers csClient) {
        this.csClient = csClient;
    }

}
