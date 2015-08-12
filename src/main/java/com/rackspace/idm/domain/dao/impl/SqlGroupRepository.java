
package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.GroupDao;
import com.rackspace.idm.domain.entity.Group;
import com.rackspace.idm.domain.sql.dao.GroupRepository;
import com.rackspace.idm.domain.sql.entity.SqlGroup;
import com.rackspace.idm.domain.sql.mapper.impl.GroupMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@SQLComponent
public class SqlGroupRepository implements GroupDao {

    @Autowired
    IdentityConfig config;

    @Autowired
    GroupMapper mapper;

    @Autowired
    GroupRepository groupRepository;

    @Override
    public String getNextGroupId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public Iterable<Group> getGroups() {
        return mapper.fromSQL(groupRepository.findAll());
    }

    @Override
    public Group getGroupById(String groupId) {
        return mapper.fromSQL(groupRepository.findOne(groupId));
    }

    @Override
    public Group getGroupByName(String groupName) {
        return mapper.fromSQL(groupRepository.findByName(groupName));
    }

    @Override
    @Transactional
    public void deleteGroup(String groupId) {
        groupRepository.delete(groupId);
    }

    @Override
    @Transactional
    public void addGroup(Group group) {
        SqlGroup sqlGroup = mapper.toSQL(group);
        //TODO: Keystone requires domain_id to be specified
        sqlGroup.setDomainId(config.getReloadableConfig().getGroupDefaultDomainId());
        groupRepository.save(sqlGroup);
    }

    @Override
    @Transactional
    public void updateGroup(Group group) {
        groupRepository.save(mapper.toSQL(group, groupRepository.findOne(group.getGroupId())));
    }

}
