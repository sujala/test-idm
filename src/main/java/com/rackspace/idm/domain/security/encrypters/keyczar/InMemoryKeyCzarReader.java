package com.rackspace.idm.domain.security.encrypters.keyczar;

import org.keyczar.exceptions.KeyczarException;
import org.keyczar.interfaces.KeyczarReader;

import java.util.*;

public class InMemoryKeyCzarReader implements KeyczarReader {

    private final KeyMetadata metadata;
    private final Map<Integer, KeyVersion> versionMap;
    private final org.keyczar.KeyMetadata kcMetaData;
    private final Date retrieved;
    private final Map info;

    // Info data
    private static final String UPDATED = "updated";
    private static final String RETRIEVED = "retrieved";
    private static final String SIZE = "size";
    private static final String VERSION = "version";
    private static final String STATUS = "status";
    private static final String CREATED = "created";
    private static final String KEYS = "keys";

    public InMemoryKeyCzarReader(KeyMetadata metadata, List<KeyVersion> versions) {
        this.retrieved = new Date();

        // Stores metadata.
        this.metadata = metadata;
        kcMetaData = org.keyczar.KeyMetadata.read(metadata.getData());

        // Stores versions map.
        final Map<Integer, KeyVersion> versionsHashMap = new HashMap<Integer, KeyVersion>();
        for (KeyVersion version : versions) {
            versionsHashMap.put(version.getVersion(), version);
        }
        versionMap = Collections.unmodifiableMap(versionsHashMap);

        // Stores info data.
        final Map<String, Object> infoMap = new HashMap<String, Object>();
        Date timestamp = metadata.getTimestamp();
        infoMap.put(UPDATED, timestamp == null ? null : timestamp.toGMTString());
        infoMap.put(RETRIEVED, retrieved.toGMTString());
        infoMap.put(SIZE, kcMetaData.getVersions().size());

        final List<Map<String, Object>> keys = new ArrayList<Map<String, Object>>();
        for (org.keyczar.KeyVersion version: kcMetaData.getVersions()) {
            timestamp = versionMap.get(version.getVersionNumber()).getTimestamp();

            final Map<String, Object> key = new HashMap<String, Object>();
            key.put(VERSION, version.getVersionNumber());
            key.put(STATUS, version.getStatus().toString());
            key.put(CREATED, timestamp == null ? null : timestamp.toGMTString());
            keys.add(key);
        }
        infoMap.put(KEYS, keys);

        info = Collections.unmodifiableMap(infoMap);
    }

    @Override
    public String getKey(int versionNum) throws KeyczarException {
        final KeyVersion version = versionMap.get(versionNum);
        return version == null ? null : version.getData();
    }

    @Override
    public String getKey() throws KeyczarException {
        final int primaryKeyVersionNum = kcMetaData.getPrimaryVersion().getVersionNumber();
        return getKey(primaryKeyVersionNum);
    }

    @Override
    public String getMetadata() throws KeyczarException {
        return metadata.getData();
    }

    public Map<String, Object> getMetadataInfo() {
        return info;
    }

}
