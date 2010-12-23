package com.rackspace.idm.docs;

import java.io.IOException;

import javax.xml.bind.JAXBException;

import com.rackspace.idm.jaxb.Version;
import com.rackspace.idm.jaxb.VersionStatus;
import com.rackspace.idm.jaxb.Versions;

public class VersionSampleGenerator extends SampleGenerator {
    private VersionSampleGenerator() {
        super();
    }
    
    public static void main(String[] args) throws JAXBException, IOException {
        VersionSampleGenerator sampleGen = new VersionSampleGenerator();

        sampleGen.marshalToFiles(sampleGen.getVersion(), "version");
        sampleGen.marshalToFiles(sampleGen.getVersions(), "versions");
    }
    
    public Version getVersion() {
        Version version = of.createVersion();
        version.setDocURL("http://docs.rackspacecloud.com/idm/api/v1.0/idm-devguide-20101015.pdf");
        version.setId("v1.0");
        version.setStatus(VersionStatus.CURRENT);
        version.setWadl("http://docs.rackspacecloud.com/idm/api/v1.0/application.wadl");
        return version;
    }
    
    public Version getVersion2() {
        Version version = of.createVersion();
        version.setDocURL("http://docs.rackspacecloud.com/idm/api/v1.1/idm-devguide-20101015.pdf");
        version.setId("v1.1");
        version.setStatus(VersionStatus.BETA);
        version.setWadl("http://docs.rackspacecloud.com/idm/api/v1.1/application.wadl");
        return version;
    }
    
    public Version getVersion3() {
        Version version = of.createVersion();
        version.setDocURL("http://docs.rackspacecloud.com/idm/api/v0.9/idm-devguide-20101015.pdf");
        version.setId("v0.9");
        version.setStatus(VersionStatus.DEPRECATED);
        version.setWadl("http://docs.rackspacecloud.com/idm/api/v0.9/application.wadl");
        return version;
    }
    
    public Versions getVersions() {
        Versions versions = of.createVersions();
        versions.getVersions().add(getVersion3());
        versions.getVersions().add(getVersion());
        versions.getVersions().add(getVersion2());
        return versions;
    }
}
