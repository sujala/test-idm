
package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.GroupDao;
import com.rackspace.idm.domain.entity.Group;
import com.rackspace.idm.domain.migration.ChangeType;
import com.rackspace.idm.domain.migration.sql.event.SqlMigrationChangeApplicationEvent;
import com.rackspace.idm.domain.sql.dao.GroupRepository;
import com.rackspace.idm.domain.sql.entity.SqlGroup;
import com.rackspace.idm.domain.sql.mapper.impl.GroupMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@SQLComponent
public class SqlGroupRepository implements GroupDao {

    @Autowired
    private IdentityConfig config;

    @Autowired
    private GroupMapper mapper;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional
    public void addGroup(Group group) {
        SqlGroup sqlGroup = mapper.toSQL(group);
        //TODO: Keystone requires domain_id to be specified
        sqlGroup.setDomainId(config.getReloadableConfig().getGroupDefaultDomainId());
        sqlGroup = groupRepository.save(sqlGroup);

        final Group newGroup = mapper.fromSQL(sqlGroup, group);
        applicationEventPublisher.publishEvent(new SqlMigrationChangeApplicationEvent(this, ChangeType.ADD, newGroup.getUniqueId(), mapper.toLDIF(newGroup)));
    }

    @Override
    @Transactional
    public void updateGroup(Group group) {
        final SqlGroup sqlGroup = groupRepository.save(mapper.toSQL(group, groupRepository.findOne(group.getGroupId())));

        final Group newGroup = mapper.fromSQL(sqlGroup, group);
        applicationEventPublisher.publishEvent(new SqlMigrationChangeApplicationEvent(this, ChangeType.MODIFY, newGroup.getUniqueId(), mapper.toLDIF(newGroup)));
    }

    @Override
    @Transactional
    public void deleteGroup(String groupId) {
        final SqlGroup sqlGroup = groupRepository.findOne(groupId);
        groupRepository.delete(groupId);

        final Group newGroup = mapper.fromSQL(sqlGroup);
        applicationEventPublisher.publishEvent(new SqlMigrationChangeApplicationEvent(this, ChangeType.DELETE, newGroup.getUniqueId(), null));
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
    public String getNextGroupId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

}
