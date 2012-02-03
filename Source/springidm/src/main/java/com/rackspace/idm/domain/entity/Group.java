package com.rackspace.idm.domain.entity;

/**
 * Created by IntelliJ IDEA.
 * User: matt.colton
 * Date: 1/31/12
 * Time: 12:55 PM
 * To change this template use File | Settings | File Templates.
 */
public class Group implements Auditable {

    private String uniqueId = null;
    private Integer groupId = null;
    private String name = null;
    private String description = null;

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getAuditContext() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
