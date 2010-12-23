"""
Usage: fab -u your_user_name <deploy_method_name>
Dependency: Fabric (fabfile.org)
"""

from fabric.api import env, roles, run, put, sudo, cd, hosts, local, settings
from fabric.contrib.files import exists

###############################################################################
app_version = '1.0'
###############################################################################
schema_ldif = '99-user.ldif'
data_ldif = 'combined.ldif'
########################################
built_war = 'idm-1.0.0-SNAPSHOT.war'
########################################
app_name = "v%s" % app_version
trusted_app_name = "v%st" % app_version
########################################
app_war = "%s.war" % app_name
trusted_app_war = "%s.war" % trusted_app_name
###############################################################################

idm_hosts = {
    'dev':{'data':'172.17.16.82', 'app':'172.17.16.83'},
    'qa':{'data':'172.17.16.84', 'app':'172.17.16.85'}
}

# Remote server's LDAP server installation location
ds_root_dev = '/opt/OpenDS-2.3.0-build003'
ds_root_qa = '/opt/UnboundID-DS'
# Remote server's app server installation location
as_root_dev = '/usr/local/share/apache-tomcat-6.0.26'
as_root_qa = '/usr/local/share/apache-tomcat-6.0.26'

def _deploy_ds(ds_root):
    put("ldap/ldif/*.ldif", '/tmp')        
    sudo("mv /tmp/%s %s/config/schema/" % (schema_ldif, ds_root,))
    with cd(ds_root):
        sudo('bin/stop-ds')
        result = sudo("bin/import-ldif --ldifFile /tmp/%s -n userRoot --clearBackend -R barf.out"
            % data_ldif)
        print(result)
        if not result.failed:
            sudo('bin/start-ds')
    if exists("/tmp/%s" % data_ldif):
        sudo("rm /tmp/%s" % data_ldif)

def _deploy_as(as_root, config_setup):
    put("target/%s" % built_war, '/tmp')
    with cd("%s/webapps" % as_root):
        sudo('../bin/shutdown.sh')
        sudo("mv /tmp/%s %s" % (built_war, app_war,))
        sudo("cp %s %s" % (app_war, trusted_app_war,))
        sudo("rm -rf %s/" % app_name)
        sudo("rm -rf %s/" % trusted_app_name)
        sudo("unzip %s -d %s" % (app_war, app_name,))
        sudo("unzip %s -d %s" % (trusted_app_war, trusted_app_name,))
        config_setup(as_root)
        sudo('../bin/startup.sh')

def _setup_dev_config(as_root):
    with cd("%s/webapps/%s/WEB-INF/classes" % (as_root, app_name)):
        sudo("rm %s" % 'config.properties.rackauth')
        sudo("rm %s" % 'config.properties.qa')
        sudo("rm %s" % 'config.properties.rackauth.qa')
    with cd("%s/webapps/%s/WEB-INF/classes" % (as_root, trusted_app_name)):
        sudo("rm %s" % 'config.properties')
        sudo("rm %s" % 'config.properties.qa')
        sudo("rm %s" % 'config.properties.rackauth.qa')
        sudo("mv %s %s" % ('config.properties.rackauth', 'config.properties'))

def _setup_qa_config(as_root):
    with cd("%s/webapps/%s/WEB-INF/classes" % (as_root, app_name)):
        sudo("rm %s" % 'config.properties')
        sudo("rm %s" % 'config.properties.rackauth')
        sudo("rm %s" % 'config.properties.rackauth.qa')
        sudo("mv %s %s" % ('config.properties.qa', 'config.properties',))
    with cd("%s/webapps/%s/WEB-INF/classes" % (as_root, trusted_app_name)):
        sudo("rm %s" % 'config.properties')
        sudo("rm %s" % 'config.properties.qa')
        sudo("rm %s" % 'config.properties.rackauth')
        sudo("mv %s %s" % ('config.properties.rackauth.qa', 'config.properties',))

"""
Commands to be invoked 
"""

def build(skip_test=False):
    skip_test_str = "-DskipTests=true" if skip_test else ""
    result = local('mvn clean package %s' % skip_test_str, capture=False)
    if result.failed:
        abort('Aborting: Build failed.')

def build_skiptests():
    return build(True)

@hosts(idm_hosts['dev']['data'])
def deploy_dev_data():
    _deploy_ds(ds_root_dev)

@hosts(idm_hosts['dev']['app'])
def deploy_dev_app():
    _deploy_as(as_root_dev, _setup_dev_config)

@hosts(idm_hosts['qa']['data'])
def deploy_qa_data():
    _deploy_ds(ds_root_qa)

@hosts(idm_hosts['qa']['app'])
def deploy_qa_app():
    _deploy_as(as_root_qa, _setup_qa_config)