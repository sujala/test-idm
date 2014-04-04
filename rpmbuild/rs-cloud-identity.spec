%{!?version : %define version __VERSION__}
%{!?release : %define release __RELEASE__}

Name:           rs-cloud-identity
Version:        %{version}
Release:        %{release}
Summary:        Cloud Identity Application

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
mkdir -p %{buildroot}/etc/idm
# TODO: true up packaged configs with those from written by Chef
# cp src/main/config/DEV/* %{buildroot}/etc/idm

mkdir -p %{buildroot}/var/log/idm

mkdir -p %{buildroot}/usr/share/tomcat7/webapps
cp idm-%{version}-%{release}.war %{buildroot}/usr/share/tomcat7/webapps/ROOT.war

%files
%config(noreplace) /etc/idm
%dir /var/log/idm
/usr/share/tomcat7/webapps/ROOT.war

%changelog

