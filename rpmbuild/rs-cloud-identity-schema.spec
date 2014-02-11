%{!?version : %define version __VERSION__}
%{!?release : %define release __RELEASE__}

Name:           rs-cloud-identity-schema
Version:        %{version}
Release:        %{release}
Summary:        Cloud Identity LDAP Schema

Group:          Applications/File
License:        (C) Copyright Rackspace
URL:            https://github.rackspace.com/cloud-identity-dev/cloud-identity
Source0:        master.tar.gz

BuildArch:      noarch
#BuildRequires:
#Requires:

%description
Cloud Identity Project

%prep
%setup -n master -q

%build

%install
mkdir -p %{buildroot}/opt/CA/Directory/dxserver/config/schema
cp ldap/config/schema/rackschema.dxc %{buildroot}/opt/CA/Directory/dxserver/config/schema

%files
/opt/CA/Directory/dxserver/config/schema/rackschema.dxc

%changelog

