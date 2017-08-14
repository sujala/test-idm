package com.rackspace.idm.domain.config;

import com.google.common.cache.CacheBuilder;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.guava.GuavaCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfiguration {

    public final static String CLIENT_ROLE_CACHE_BY_ID = "clientRoleCacheById";
    public final static String CLIENT_ROLE_CACHE_BY_NAME = "clientRoleCacheByName";

    @Autowired
    IdentityConfig identityConfig;

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(getClientRoleCache(CLIENT_ROLE_CACHE_BY_ID), getClientRoleCache(CLIENT_ROLE_CACHE_BY_NAME)));
        return cacheManager;
    }

    public GuavaCache getClientRoleCache(String name) {
        CacheBuilder builder = createClientRoleCacheBuilder();
        return new GuavaCache(name, builder.build());
    }

    /**
     * Extracting this as a separate method to allow for explicit testing of generating the builder based on identity
     * config properties
     *
     * @return
     */
    private CacheBuilder createClientRoleCacheBuilder() {
        Duration ttl = identityConfig.getStaticConfig().getClientRoleByIdCacheTtl();
        int size = identityConfig.getStaticConfig().getClientRoleByIdCacheSize();

        return CacheBuilder.newBuilder()
                .maximumSize(size)
                .expireAfterWrite(ttl.toMillis(), TimeUnit.MILLISECONDS);
    }

}