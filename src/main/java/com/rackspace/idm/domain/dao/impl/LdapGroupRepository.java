package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.dao.GroupDao;
import com.rackspace.idm.domain.entity.Group;
import com.unboundid.ldap.sdk.Filter;
import org.springframework.stereotype.Component;

/**
 * Created by IntelliJ IDEA.
 * User: matt.colton
 * Date: 1/31/12
 * Time: 2:33 PM
 * To change this template use File | Settings | File Templates.
 */
@Component
public class LdapGroupRepository extends LdapGenericRepository<Group> implements GroupDao {

    public String getBaseDn() {
        return GROUP_BASE_DN;
    }

    public String getLdapEntityClass(){
        return OBJECTCLASS_CLOUDGROUP;
    }

    public String getSortAttribute() {
        return ATTR_ID;
    }

    @Override
    public void addGroup(Group group) {
        addObject(group);
    }

    @Override
    public void updateGroup(Group group) {
        Group oldGroup = getGroupById(group.getGroupId());
        group.setLdapEntry(oldGroup.getLdapEntry());
        updateObject(group);
    }

    @Override
    public void deleteGroup(String groupId) {
        deleteObject(searchFilterGetGroupById(groupId));
    }

    @Override
    public Group getGroupById(String groupId) {
        return getObject(searchFilterGetGroupById(groupId));
    }

    @Override
    public Group getGroupByName(String name){
        Iterable<Group> groups = getObjects(searchFilterGetGroupByName(name));
        if (groups.iterator().hasNext()) {
            return groups.iterator().next();
        } else {
            return null;
        }
    }

    @Override
    public Iterable<Group> getGroups() {
        return getObjects(searchFilterGetGroups());
    }

    @Override
    public String getNextGroupId() {
        return getNextId( NEXT_GROUP_ID);
    }

    private Filter searchFilterGetGroupById(String groupId) {
        return new LdapRepository.LdapSearchBuilder()
                .addEqualAttribute(LdapRepository.ATTR_ID, groupId)
                .addEqualAttribute(LdapRepository.ATTR_OBJECT_CLASS, LdapRegionRepository.OBJECTCLASS_CLOUDGROUP).build();
    }

    private Filter searchFilterGetGroupByName(String name) {
        return new LdapRepository.LdapSearchBuilder()
                .addEqualAttribute(LdapRepository.ATTR_GROUP_NAME, name)
                .addEqualAttribute(LdapRepository.ATTR_OBJECT_CLASS, LdapRegionRepository.OBJECTCLASS_CLOUDGROUP).build();
    }

    private Filter searchFilterGetGroups() {
        return new LdapRepository.LdapSearchBuilder()
                .addEqualAttribute(LdapRegionRepository.ATTR_OBJECT_CLASS, LdapRegionRepository.OBJECTCLASS_CLOUDGROUP).build();
    }
}
