%define version BUILD_VERSION
%define build BUILD_RELEASE
%define release 1

Summary: Cloud Identity War application
Name: rs-identity
Version: %{version}
Release: %{release}
Source0: cloud-identity-%{version}.tar.gz
Vendor: Rackspace, Inc.
Packager: Werner Mendizabal <werner.mendizabal@rackspace.com>
Url: https://github.rackspace.com/cloud-identity-dev/cloud-identity
License: (C) Copyright Rackspace
Group: Tomcat Application
BuildRoot: %{_tmppath}/cloud-identity-%{version}-%{release}-buildroot
BuildArch: noarch
Requires: tomcat7

%description
Cloud Identity Project

%prep
%setup -n cloud-identity-%{version} -q

%install
mkdir -p $RPM_BUILD_ROOT/opt/idm/artifacts
mkdir -p $RPM_BUILD_ROOT/etc/idm/config
mkdir -p $RPM_BUILD_ROOT/var/log/idm
touch $RPM_BUILD_ROOT/var/log/idm/analytics.log
touch $RPM_BUILD_ROOT/var/log/idm/audit.log
touch $RPM_BUILD_ROOT/var/log/idm/idm.log

mkdir -p $RPM_BUILD_ROOT/srv/tomcat7/webapps

cp %_topdir/BUILD/cloud-identity-%{version}/idm-%{version}-%{build}.war $RPM_BUILD_ROOT/opt/idm/artifacts

cp %_topdir/BUILD/cloud-identity-%{version}/base.idm.properties $RPM_BUILD_ROOT/etc/idm/config
cp %_topdir/BUILD/cloud-identity-%{version}/idm.properties $RPM_BUILD_ROOT/etc/idm/config
cp %_topdir/BUILD/cloud-identity-%{version}/idm.secrets.properties $RPM_BUILD_ROOT/etc/idm/config
cp %_topdir/BUILD/cloud-identity-%{version}/log4j.xml $RPM_BUILD_ROOT/etc/idm/config

ln -sf /etc/idm/config %{buildroot}/opt/idm/config
ln -sf /opt/idm/artifacts/idm-%{version}-%{build}.war %{buildroot}/srv/tomcat7/webapps/idm.war

%files
%defattr(-,root,root)
/opt/idm/artifacts/idm-%{version}-%{build}.war
/srv/tomcat7/webapps/idm.war

%config(noreplace) /etc/idm/config/*
%config(noreplace) %attr(0750,tomcat,tomcat) /var/log/idm/*
/opt/idm/config

%pre
echo "Installation started."

%post
echo "Installation complete."
