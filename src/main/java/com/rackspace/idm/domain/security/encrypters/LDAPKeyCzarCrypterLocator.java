package com.rackspace.idm.domain.security.encrypters;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.security.encrypters.keyczar.*;
import com.rackspace.idm.domain.security.encrypters.keyczar.KeyMetadata;
import com.rackspace.idm.domain.security.encrypters.keyczar.KeyVersion;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.keyczar.*;
import org.keyczar.exceptions.KeyczarException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.List;

public class LDAPKeyCzarCrypterLocator implements CacheableKeyCzarCrypterLocator {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * hardcode single meta for now...
     */
    private static final String DN_META = "meta";

    @Autowired
    private KeyCzarKeyMetadataDao keyCzarKeyMetadataDao;

    @Autowired
    private KeyCzarKeyVersionDao keyCzarKeyVersionDao;

    @Autowired
    private IdentityConfig identityConfig;

    private static final DateTimeFormatter dateLoggerFormat = ISODateTimeFormat.dateTime();

    private Supplier<CrypterCache> crypterCacheSupplier = new CrypterCacheSupplier();

    private volatile Supplier<CrypterCache> memoizedCrypterCache;

    @PostConstruct
    public void init() {
        //only load (and fail if can't load) keys on startup if AE is enabled
        if (identityConfig.getStaticConfig().getFeatureAETokensDecrypt()) {
            resetCache(); //load the cache with the keys on node start
            if (memoizedCrypterCache == null) {
                //if the cache can't be loaded, throw a hard error. The node must not start if we can't load the keys
                throw new CrypterCacheCreationException("Error initializing Crypter. Please see error logs for more information");
            }
        }
    }

    @Override
    public Crypter getCrypter() {
        return memoizedCrypterCache.get().crypter;
    }

    @Override
    public com.rackspace.docs.identity.api.ext.rax_auth.v1.KeyMetadata getCacheInfo() {
        return memoizedCrypterCache.get().inMemoryKeyCzarReader.getMetadataInfo();
    }

    @Override
    public void resetCache() {
        try {
            if (replaceCacheSupplierIfNecessary()) {
                /*
                now that memoized is updated and being used, record the change in directory. Must only record the change
                AFTER the memoized variable is updated. While it is not ideal to use the newest key data and fail to
                record it, it won't cause errors. However, if we recorded that we are using the newest version, but
                aren't, then it could cause significant issues (e.g. - a new key could be promoted to primary when this
                node doesn't actually know about it).
                */
                recordCachedVersion();
            }
        } catch (Exception e) {
            //when resetting the cache, catch any thrown errors, log, but continue using existing cache entry
            logger.error(String.format("Error repopulating AE meta cache. Please check earlier logs for more information"), e);
        }
    }

    /**
     * Replace, if necessary, the memoizedCrypterCache volatile instance variable with a new instance that will retrieve the new
     * set of keys from LDAP. Do NOT replace the variable until after the new keys are loaded to guarantee usability.
     *
     * @return whether the cache needed to be reloaded
     */
    private boolean replaceCacheSupplierIfNecessary() {
        logger.debug(String.format("Refreshing AE Metadata cache if necessary"));

        boolean replaced = false;

        DateTime cacheVersionCreated = null;
        if (memoizedCrypterCache != null) {
            CrypterCache currentCache = memoizedCrypterCache.get();
            KeyMetadata cachedMetadata = currentCache.inMemoryKeyCzarReader.getStoredMetadata();
            cacheVersionCreated = new DateTime(cachedMetadata.getCreated());
        }

        //first verify need to reset by checking if cached meta == persisted meta
        KeyMetadata persistedMetadata = keyCzarKeyMetadataDao.getKeyMetadataByName(DN_META);
        if (persistedMetadata != null) {
            DateTime persistedVersionCreated = new DateTime(persistedMetadata.getCreated());

            if (cacheVersionCreated == null || cacheVersionCreated.isBefore(persistedVersionCreated)) {
                logger.warn(String.format("Detected AE Key Meta modification. Cached Creation Date: '%s'; New Creation Date: '%s'", cacheVersionCreated != null ? dateLoggerFormat.print(cacheVersionCreated) : null, dateLoggerFormat.print(persistedVersionCreated)));
                try {
                    Supplier<CrypterCache> newCacheSupplier = generateCacheSupplier();

                    //preload cache to verify can load keys. Don't want to replace live cache (the volatile instance
                    // variable) unless we know the new one is good.
                    KeyMetadata storedMetadata = newCacheSupplier.get().inMemoryKeyCzarReader.getStoredMetadata();
                    DateTime newCacheVersionCreated = new DateTime(storedMetadata.getCreated());

                    //now replace the volatile variable with the newly initialized cache supplier
                    memoizedCrypterCache = newCacheSupplier;

                    logger.warn(String.format("Loaded/Reloaded AE Key MetaData cache. Newly Cached MetaData was created at: '%s'", dateLoggerFormat.print(newCacheVersionCreated)));
                    replaced = true;
                } catch (Exception e) {
                    //when resetting the cache, catch any thrown errors, log, but continue using existing cache entry
                    logger.error(String.format("Error loading new AE Key Meta data with creation date '%s'. Using existing cached data from '%s'", dateLoggerFormat.print(persistedVersionCreated), dateLoggerFormat.print(cacheVersionCreated)), e);
                }
            } else {
                logger.debug(String.format("AE Key Meta is up to date. Cached Creation Date: '%s'", dateLoggerFormat.print(cacheVersionCreated)));
            }
        }
        return replaced;
    }

    /**
     * Create a new supplier and verify can load keys
     *
     * @return true if the supplier was replaced
     */
    private Supplier<CrypterCache> generateCacheSupplier() {
            Supplier<CrypterCache> newMemCrypterCacheSupplier = Suppliers.memoize(crypterCacheSupplier);
            return newMemCrypterCacheSupplier;
    }

    //TODO: Update LDAP with the newly cached version
    public void recordCachedVersion() {
        logger.info(String.format("Recording newly loaded AE Meta data Cache"));

        // CrypterCache cache = memoizedCrypterCache.get();
        logger.info(String.format("Would record newly loaded meta now"));

    }

    /**
     * Generates a new CrypterCache by loading key data from LDAP.
     */
    private class CrypterCacheSupplier implements Supplier<CrypterCache> {
        @Override
        public CrypterCache get() {
            try {
                final KeyMetadata meta = keyCzarKeyMetadataDao.getKeyMetadataByName(DN_META);
                final List<KeyVersion> versions = keyCzarKeyVersionDao.getKeyVersionsForMetadata(DN_META);
                final InMemoryKeyCzarReader imr = new InMemoryKeyCzarReader(meta, versions);
                CrypterCache newCache = new CrypterCache(imr);
                return newCache;
            } catch (Exception e) {
                throw new CrypterCacheCreationException("KR-001", "Error loading new AE Key Meta data. Leaving existing cached key meta in place.", e);
            }
        }
    }

    private class CrypterCache {
        private final Crypter crypter;
        private final InMemoryKeyCzarReader inMemoryKeyCzarReader;

        private CrypterCache(InMemoryKeyCzarReader inMemoryKeyCzarReader) {
            this.inMemoryKeyCzarReader = inMemoryKeyCzarReader;
            try {
                this.crypter = new Crypter(inMemoryKeyCzarReader);
            } catch (KeyczarException e) {
                throw new CrypterCacheCreationException("KEYCZAR-LDAP-001", "Error reading keys.", e);
            }
        }
    }
}