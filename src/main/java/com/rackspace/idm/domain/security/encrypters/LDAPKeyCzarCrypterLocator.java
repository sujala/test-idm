package com.rackspace.idm.domain.security.encrypters;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.security.signoff.KeyCzarAPINodeSignoffRepository;
import com.rackspace.idm.domain.security.signoff.LdapAPINodeSignoff;
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
import java.util.Date;
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
    private KeyCzarAPINodeSignoffRepository keyCzarAPINodeSignoffDao;

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
            replaceCacheSupplierIfNecessary();
        } catch (Exception e) {
            //when resetting the cache, catch any thrown errors, log, but continue using existing cache entry
            logger.error(String.format("Error repopulating AE meta cache. Please check earlier logs for more information"), e);
        }

        /*
        regardless of whether cache was updated, verify the signoff entry for this node correctly reflects the cache
        entry. Must only record the change
        AFTER the memoized variable is updated. While it is not ideal to use the newest key data and fail to
        record it, it won't cause errors. However, if we recorded that we are using the newest version, but
        aren't, then it could cause significant issues (e.g. - a new key could be promoted to primary when this
        node doesn't actually know about it).
        */
        try {
            recordCachedVersionIfNecessary();
        } catch (Exception e) {
            //when resetting the cache, catch any thrown errors, log, but continue using existing cache entry
            logger.error(String.format("Error recording AE Key Signoff for node. Please check earlier logs for more information"), e);
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

    private void recordCachedVersionIfNecessary() {
        if (!identityConfig.getReloadableConfig().getAESyncSignOffEnabled()) {
            return; //no sync
        }

        String nodeName = identityConfig.getReloadableConfig().getAENodeNameForSignoff();
        logger.debug(String.format("Validating registered AE key Signoff for node '%s'", nodeName));

        CrypterCache cache = memoizedCrypterCache.get();

        //retrieve the registered signoff entry for this node
        LdapAPINodeSignoff signoff = keyCzarAPINodeSignoffDao.getByNodeAndMetaName(DN_META, nodeName);

        DateTime currentSignOffDate = signoff != null && signoff.getCachedMetaCreatedDate() != null ? new DateTime(signoff.getCachedMetaCreatedDate()) : null;
        String prettyCurrentSignoffDate = currentSignOffDate == null ? null : dateLoggerFormat.print(currentSignOffDate);

        if (cache != null) {
            //make sure signoff reflects cache
            DateTime cachedMetaCreationDate = new DateTime(cache.inMemoryKeyCzarReader.getStoredMetadata().getCreated());
            DateTime cachedMetaRetrievedDate = new DateTime(cache.inMemoryKeyCzarReader.getRetrieved());
            String prettyCachedCreationDate = dateLoggerFormat.print(cachedMetaCreationDate);

            if (currentSignOffDate == null
                    || !cachedMetaCreationDate.equals(currentSignOffDate)) {

                //signoff is not synchronized with the loaded cache. Update info.
                if (currentSignOffDate != null
                        && cachedMetaCreationDate.isBefore(currentSignOffDate)) {
                    //the signoff in CA for this node represents a date LATER than what the node has loaded in cache! This should not happen!
                    logger.error(String.format("Illegal State! Registered AE key signoff for node '%s' is before the loaded cache! Signoff creation date is set to '%s' while loaded cache is '%s'. Updating signoff to reflect cache.", nodeName, prettyCurrentSignoffDate, prettyCachedCreationDate));
                } else {
                    //normal update
                    logger.debug(String.format("Registered AE key signoff for node '%s' is out of date with the loaded cache. Updating from meta with creation date '%s' to meta with creation date '%s'", nodeName, prettyCurrentSignoffDate, prettyCachedCreationDate));
                }

                if (signoff == null) {
                    //create new if needed
                    signoff = new LdapAPINodeSignoff();
                }

                signoff.setCachedMetaCreatedDate(cachedMetaCreationDate.toDate());
                signoff.setKeyMetadataId(DN_META);
                signoff.setLoadedDate(cachedMetaRetrievedDate.toDate());
                signoff.setNodeName(nodeName);
                keyCzarAPINodeSignoffDao.addOrUpdateObject(signoff);

                logger.warn(String.format("Successfully updated AE key signoff for node '%s'. Updated from meta with creation date '%s' to meta with creation date '%s'", nodeName, prettyCurrentSignoffDate, prettyCachedCreationDate));
            } else {
                logger.debug(String.format("Registered AE key signoff for node '%s' is up to date. Using meta with creation date '%s'", nodeName, prettyCachedCreationDate));
            }
        } else if (signoff != null) {
            //delete the signoff object because the cache is empty (e.g. - AE tokens have become disabled)
            logger.warn(String.format("Node '%s' has an empty AE Key Cache, but associated signoff record shows a sign off for version '%s' . Removing AE Signoff Record.", nodeName, prettyCurrentSignoffDate));
            keyCzarAPINodeSignoffDao.deleteObject(signoff);
        } else {
            //signup and cache are null so this is a no-op
            logger.debug(String.format("There is no AE Token Signoff record for node '%s'", nodeName));
        }
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