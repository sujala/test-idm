from fabric.api import *
import datetime

env.roledefs = {
    'dxserver' : [
        'cert-ldap1.cidm.iad2.corp.rackspace.com', 
        'cert-ldap2.cidm.iad2.corp.rackspace.com',
        'cert-ldap3.cidm.iad2.corp.rackspace.com'
    ]
}

today=datetime.date.today().isoformat()
schema_dir='/opt/CA/Directory/dxserver/config/schema'
schema='{schema_dir}/RackSchema.dxc'.format(**globals())
backup_schema='{schema_dir}/schemaBackUps/RackSchema{today}.dxc'.format(**globals())

def help():
    print "Usage:"
    print "\tfab update_schema:location='$PATH/RackSchema.dxc' -u $USERNAME"
    print "\tfab revert_schema -u $USERNAME"

@roles('dxserver')
def stop_dxserver():
    sudo('su - dsa -c "dxserver stop all"')
    
@roles('dxserver')
def start_dxserver():
    sudo('su - dsa -c "dxserver start all"')

@roles('dxserver')
def upload_schema(location=None):
    if not location:
        raise Exception('Schema filename not provided')

    put(location, '/tmp')
    sudo('su - dsa -c "cp {schema} {backup_schema}"'.format(**globals())) 
    sudo('su - dsa -c "cp /tmp/RackSchema.dxc {schema}"'.format(**globals()))

@roles('dxserver')
def resort_backup():
    sudo('su - dsa -c "cp {backup_schema} {schema}"'.format(**globals())) 

def update_schema(location=None): 
    execute(upload_schema, location)
    execute(stop_dxserver)
    execute(start_dxserver)

def revert_schema(): 
    execute(resort_backup)
    execute(stop_dxserver)
    execute(start_dxserver)
