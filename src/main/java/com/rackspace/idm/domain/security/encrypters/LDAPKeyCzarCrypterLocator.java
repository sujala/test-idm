package com.rackspace.idm.domain.security.encrypters;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.rackspace.idm.domain.security.encrypters.keyczar.*;
import com.rackspace.idm.domain.security.encrypters.keyczar.KeyMetadata;
import com.rackspace.idm.domain.security.encrypters.keyczar.KeyVersion;
import com.rackspace.idm.exception.ErrorCodeIdmException;
import org.keyczar.*;
import org.keyczar.exceptions.KeyczarException;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class LDAPKeyCzarCrypterLocator implements CacheableKeyCzarCrypterLocator {

    /**
     * hardcode single meta for now...
     */
    private static final String DN_META = "meta";

    @Autowired
    private KeyCzarKeyMetadataDao keyCzarKeyMetadataDao;

    @Autowired
    private KeyCzarKeyVersionDao keyCzarKeyVersionDao;

    private static class CrypterCache {
        private final Crypter crypter;
        private final InMemoryKeyCzarReader inMemoryKeyCzarReader;

        private CrypterCache(InMemoryKeyCzarReader inMemoryKeyCzarReader) {
            this.inMemoryKeyCzarReader = inMemoryKeyCzarReader;
            try {
                this.crypter = new Crypter(inMemoryKeyCzarReader);
            } catch (KeyczarException e) {
                throw new ErrorCodeIdmException("KEYCZAR-LDAP-001", "Error reading keys.");
            }
        }
    }

    private Supplier<CrypterCache> crypterCacheSupplier = new Supplier<CrypterCache>() {
        @Override
        public CrypterCache get() {
            final KeyMetadata meta = keyCzarKeyMetadataDao.getKeyMetadataByName(DN_META);
            final List<KeyVersion> versions = keyCzarKeyVersionDao.getKeyVersionsForMetadata(DN_META);
            final InMemoryKeyCzarReader imr = new InMemoryKeyCzarReader(meta, versions);
            return new CrypterCache(imr);
        }
    };

    private volatile Supplier<CrypterCache> memorizedCrypterCache = Suppliers.memoize(crypterCacheSupplier);

    @Override
    public Crypter getCrypter() {
        return memorizedCrypterCache.get().crypter;
    }

    @Override
    public com.rackspace.docs.identity.api.ext.rax_auth.v1.KeyMetadata getCacheInfo() {
        return memorizedCrypterCache.get().inMemoryKeyCzarReader.getMetadataInfo();
    }

    @Override
    public void resetCache() {
        memorizedCrypterCache = Suppliers.memoize(crypterCacheSupplier);
    }

}
