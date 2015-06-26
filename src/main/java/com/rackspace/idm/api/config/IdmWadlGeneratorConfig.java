package com.rackspace.idm.api.config;

import com.sun.jersey.api.wadl.config.WadlGeneratorConfig;
import com.sun.jersey.api.wadl.config.WadlGeneratorDescription;
import com.sun.jersey.server.wadl.generators.WadlGeneratorApplicationDoc;
import com.sun.jersey.server.wadl.generators.WadlGeneratorGrammarsSupport;
import com.sun.jersey.server.wadl.generators.resourcedoc.WadlGeneratorResourceDocSupport;

import java.util.List;

public class IdmWadlGeneratorConfig extends WadlGeneratorConfig {

    @Override
    public List<WadlGeneratorDescription> configure() {
        return generator(WadlGeneratorApplicationDoc.class).prop(
            "applicationDocsStream", "application-doc.xml").generator(
            WadlGeneratorResourceDocSupport.class).prop("resourceDocStream",
            "resourcedoc.xml").descriptions();
    }

}
