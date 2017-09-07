package com.rackspace.idm.modules.usergroups.dao;

import com.rackspace.idm.annotation.LDAPComponent;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.impl.LdapGenericRepository;
import com.rackspace.idm.modules.usergroups.entity.UserGroup;
import com.unboundid.ldap.sdk.Filter;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import static com.rackspace.idm.modules.usergroups.Constants.*;

@LDAPComponent
public class LdapUserGroupRepository extends LdapGenericRepository<UserGroup> implements UserGroupDao {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private IdentityConfig identityConfig;

    @Override
    public String getBaseDn() {
        return USER_GROUP_BASE_DN;
    }

    @Override
    public String getSortAttribute() {
        return ATTR_NAME;
    }

    @Override
    public void addGroup(UserGroup group) {
        group.setId(getUuid());
        addObject(group);
    }

    @Override
    public Iterable<UserGroup> getGroupsForDomain(String domainId) {
        return getObjects(searchByDomainId(domainId), getBaseDn());
    }

    @Override
    public UserGroup getGroupById(String groupId) {
        return getObject(searchByIdFilter(groupId));
    }

    @Override
    public UserGroup getGroupByDomainIdAndName(String domainId, String groupName) {
        Validate.notEmpty(domainId);
        Validate.notEmpty(groupName);
        return getObject(searchByDomainIdAndNameFilter(domainId, groupName));
    }

    @Override
    public void deleteGroup(UserGroup group)  { deleteObject(group); }

    @Override
    public void updateGroup(UserGroup group) { updateObject(group); }

    Filter searchByIdFilter(String id) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_ID, id)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_USER_GROUP).build();
    }

    Filter searchByDomainIdAndNameFilter(String domainId, String name) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_DOMAIN_ID, domainId)
                .addEqualAttribute(ATTR_COMMON_NAME, name)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_USER_GROUP).build();
    }

    Filter searchByDomainId(String domainId) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_DOMAIN_ID, domainId)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_USER_GROUP).build();
    }
}
