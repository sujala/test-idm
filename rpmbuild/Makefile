VERSION = 0.1.0
RELEASE = 1
ARCH = noarch

RPMBUILD := rpmbuild \
	--define "version       ${VERSION}"       \
	--define "release       ${RELEASE}"       \
	--define "_topdir       %(pwd)/rpmbuild"  \
	--define "_rpmdir       %(pwd)/rpms"      \
	--define "_srcrpmdir    %(pwd)/rpms/src"  \
	--define "_sourcedir    %(pwd)/source"

PUBLISH_HOST 	:= 'jenkins-n01.qe.ord1.cidm.rackspace.net'
PUBLISH_DIR 	:= '/var/www/html/repos/rhel/6/dev'
PUBLISH_USER  := 'jenkins'

clean:
	rm -rf rpmbuild/ rpms/ source/ master/

prep_rpmbuild:
	mkdir -p rpmbuild
	mkdir -p rpms
	mkdir -p rpms/src
	mkdir -p source

prep_master_tarball:
	mkdir -p master
	cp -r ../build/libs/idm-${VERSION}-${RELEASE}.war master/
	cp -r ../build/config/ master/
	cp -r ../ldap master/
	tar czf source/master.tar.gz master/

rpms: prep_rpmbuild prep_master_tarball
	${RPMBUILD} -ba rs-cloud-identity.spec
	${RPMBUILD} -ba rs-cloud-identity-schema.spec

srpm: prep_rpmbuild prep_master_tarball
	${RPMBUILD} -bs rs-cloud-identity.spec
	${RPMBUILD} -bs rs-cloud-identity-schema.spec

publish:
	scp rpms/${ARCH}/*-${VERSION}-${RELEASE}.${ARCH}.rpm ${PUBLISH_USER}@${PUBLISH_HOST}:${PUBLISH_DIR}
	ssh -tt ${PUBLISH_USER}@${PUBLISH_HOST} "sudo createrepo ${PUBLISH_DIR}"
