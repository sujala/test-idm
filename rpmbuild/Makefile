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

SSO_USER      := 'identity'
PUBLISH_HOST  := 'idm-mirrors-n01.qe.ord1.cidm.rackspace.net'
PUBLISH_DIR   := "/home/${SSO_USER}/rpms"
REPO_DIR      := '/opt/mirrors/repos/upstream/identity/'

FETCH_HOST := 'd-build1.iad2.corp.rackspace.com:8100'
FETCH_URL  := "/nexus/content/repositories/releases/com/rackspace/idm/${VERSION}-${RELEASE}"

clean:
	rm -rf rpmbuild/ rpms/ source/ master/

onestep: clean fetch rpms publish

fetch:
	wget --timestamping --directory-prefix ../build/libs http://${FETCH_HOST}${FETCH_URL}/idm-${VERSION}-${RELEASE}.war

prep_rpmbuild:
	mkdir -p rpmbuild
	mkdir -p rpms
	mkdir -p rpms/src
	mkdir -p source

prep_master_tarball:
	mkdir -p master
	cp -r ../build/libs/idm-${VERSION}-${RELEASE}.war master/
	cp -r ../src master/
	cp -r ../ldap master/
	tar czf source/master.tar.gz master/

rpms: prep_rpmbuild prep_master_tarball
	${RPMBUILD} -ba rs-cloud-identity.spec
	${RPMBUILD} -ba rs-cloud-identity-schema.spec

srpm: prep_rpmbuild prep_master_tarball
	${RPMBUILD} -bs rs-cloud-identity.spec
	${RPMBUILD} -bs rs-cloud-identity-schema.spec

publish:
	scp rpms/*/*-${VERSION}-${RELEASE}.*.rpm ${SSO_USER}@${PUBLISH_HOST}:${PUBLISH_DIR}
	ssh -tt ${SSO_USER}@${PUBLISH_HOST} "\
	  cp ~/rpms/* ${REPO_DIR} && createrepo ${REPO_DIR}"
