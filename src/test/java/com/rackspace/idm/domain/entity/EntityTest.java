package com.rackspace.idm.domain.entity;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 7/11/12
 * Time: 3:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class EntityTest {

    Entity entity;

    @Test
    public void createUserEntity_createsEntityWithUserType() throws Exception {
        entity = Entity.createUserEntity("entityId");
        assertThat("correct type",entity.getEntityType(),equalTo(1));
        assertThat("correct id",entity.getEntityId(),equalTo("entityId"));
    }

    @Test
    public void createApplicationEntity_createsEntityWithApplicationType() throws Exception {
        entity = Entity.createApplicationEntity("entityId");
        assertThat("correct type",entity.getEntityType(),equalTo(2));
        assertThat("correct id",entity.getEntityId(),equalTo("entityId"));
    }
}
