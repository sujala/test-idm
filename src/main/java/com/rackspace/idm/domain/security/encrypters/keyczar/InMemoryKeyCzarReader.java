package com.rackspace.idm.domain.security.encrypters.keyczar;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.KeyVersionStatusEnum;
import com.rackspace.idm.domain.entity.KeyMetadata;
import com.rackspace.idm.domain.entity.KeyVersion;
import org.keyczar.exceptions.KeyczarException;
import org.keyczar.interfaces.KeyczarReader;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.*;

public class InMemoryKeyCzarReader implements KeyczarReader {

    private final KeyMetadata metadata;
    private final Map<Integer, KeyVersion> versionMap;
    private final org.keyczar.KeyMetadata kcMetaData;
    private final Date retrieved;
    private final com.rackspace.docs.identity.api.ext.rax_auth.v1.KeyMetadata info;

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

        // Creates info data.
        info = new com.rackspace.docs.identity.api.ext.rax_auth.v1.KeyMetadata();
        info.setCreated(convertDate(metadata.getCreated()));
        info.setRetrieved(convertDate(retrieved));
        info.setSize(kcMetaData.getVersions().size());
        for (org.keyczar.KeyVersion idx: kcMetaData.getVersions()) {
            final com.rackspace.docs.identity.api.ext.rax_auth.v1.KeyVersion data = new com.rackspace.docs.identity.api.ext.rax_auth.v1.KeyVersion();
            final KeyVersion version = versionMap.get(idx.getVersionNumber());
            data.setCreated(convertDate(version.getCreated()));
            data.setVersion(idx.getVersionNumber());
            try {
                data.setStatus(KeyVersionStatusEnum.fromValue(idx.getStatus().toString().toUpperCase()));
            } catch (Exception e) {
            }
            info.getKey().add(data);
        }
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

    public KeyMetadata getStoredMetadata() {
        return metadata;
    }

    public Date getRetrieved() {
        return retrieved;
    }

    public com.rackspace.docs.identity.api.ext.rax_auth.v1.KeyMetadata getMetadataInfo() {
        return info;
    }

    private XMLGregorianCalendar convertDate(Date date) {
        if (date == null) {
            return null;
        }
        try {
            final GregorianCalendar cal = new GregorianCalendar();
            cal.setTime(date);
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
        } catch (Exception e) {
            return null;
        }
    }

}
