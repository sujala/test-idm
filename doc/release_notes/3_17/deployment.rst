Cloud Identity 3.17.0 Release
==============================

Release Notes 
--------------

Info
------

Release Ticket  - https://jira.rax.io/browse/CID-1202
Staging Deployment Ticket - https://jira.rax.io/browse/CIDM-951
Prod Deployment Ticket - https://jira.rax.io/browse/CIDM-973

.. csv-table:: Release Progress
   :header: Milestone, Date, Status

   RC Due, Tues - Oct 30 2017 RC1 Build number: 3.17.0-1509513726184, Delivered
   QE Signoff for Staging, Thurs - Nov 2 2017 , Delivered
   Staging Release Week,  Week of Nov 6 2017, Delivered
   Signoff for Production, Nov 8 - 2017, Delivered
   Production Release Week, Week of Nov 13 2017, Delivered


Significant Changes
--------------------


Issues Resolved
----------------

Stories
--------

#. Support group assignment in v2 Federation API - https://jira.rax.io/browse/CID-1110
#. Provide service to grant role on individual tenant to a user group - https://jira.rax.io/browse/CID-1136
#. Modify domain admin change service to ignore role assignments due to group membership - https://jira.rax.io/browse/CID-1164
#. Provide service to remove role on individual tenant from a user group - https://jira.rax.io/browse/CID-1137
#. Tenant type control of identity:tenant-access policy - https://jira.rax.io/browse/CID-1194
#. Support emailDomains for IDPs - https://jira.rax.io/browse/CID-1200
#. Do not filter the service catalog for impersonation tokens of suspended users - https://jira.rax.io/browse/CID-1211
#. Repose Upgrade to 8.7.1 - https://jira.rax.io/browse/CID-1223

Defects
--------

#. Fix Error message on IDP creation - https://jira.rax.io/browse/CID-1156 

Technical Debts
----------------

#. Remove forgot password feature flag - https://jira.rax.io/browse/CID-718
#. Remove list user feature flags - https://jira.rax.io/browse/CID-715
#. Remove federation feature flags - https://jira.rax.io/browse/CID-716
#. Remove all Keystone V3 feature flags - https://jira.rax.io/browse/CID-834

Configuration Updates
----------------------

----
New
----

.. csv-table:: Configuration Changes
   :header: "Name", "Description", "DefaultValue", "Story", "File"

   tenant.prefixes.to.exclude.auto.assign.role.from, Stores a delimited list of tenant types, ,  https://jira.rax.io/browse/CID-1194, reloadable
   feature.should.display.service.catalog.for.suspended.user.impersonate.tokens, Filter (or not) the service catalog for impersonation tokens of suspended users, false,  https://jira.rax.io/browse/CID-1211, reloadable

-------
Updates
-------

.. csv-table:: Configuration Changes
   :header: "Name", "Description", "DefaultValue", "Story", "File"

   None, 

-------
Deleted
-------

These properties should be removed from the respective properties files as they are no longer used.

.. csv-table:: Deleted Configurations
   :header: "Name", "Story", "File"

   feature.forgot.pwd.enabled, https://jira.rax.io/browse/CID-718, reloadable
   feature.restrict.user.manager.list.users.usage, https://jira.rax.io/browse/CID-715, reloadable
   feature.restrict.user.manager.list.users.by.email.usage, https://jira.rax.io/browse/CID-715, reloadable
   feature.restrict.user.manager.list.users.by.name.usage, https://jira.rax.io/browse/CID-715, reloadable
   feature.support.saml.logout, https://jira.rax.io/browse/CID-716, reloadable
   feature.support.identity.provider.management, https://jira.rax.io/browse/CID-716, reloadable
   feature.forgot.pwd.enabled, https://jira.rax.io/browse/CID-718, reloadable
   feature.DefaultCloud11Service.addBaseURLKeystoneV3Data.throwError, https://jira.rax.io/browse/CID-834, reloadable
   feature.DefaultCloud20Service.addEndpointTemplateKeystoneV3Data.throwError, https://jira.rax.io/browse/CID-834, reloadable
   feature.DefaultCloud20Service.addTenantKeystoneV3Data.throwError, https://jira.rax.io/browse/CID-834, reloadable
   GAKeystoneDisabled, https://jira.rax.io/browse/CID-834, reloadable
   feature.support.v3.provisioned.user.tokens, https://jira.rax.io/browse/CID-834, reloadable


Directory Changes
------------------

----
New
----

New schema shown below ::

   schema set attribute (1.3.6.1.4.1.20988.2.1.146) = {
       name = rsEmailDomains
       ldap-names = rsEmailDomains
       equality = caseIgnoreMatch
       syntax = directoryString
       description = "Stores a set of email domains"
   };

--------
Updates
--------
Schema  update shown below ::

  schema set object-class (1.3.6.1.4.1.20988.2.2.35) = {
      name = rsExternalProvider
      ldap-names = rsExternalProvider
      subclass-of organizationalUnit
      kind = structural
      must-contain
          ou,
          labeledUri
      may-contain
          cn,
          description,
          nisPublicKey,
          userCertificate,
          rsTargetUserSource,
          rsApprovedDomainGroup,
          rsApprovedDomainIds,
          rsAuthenticationUrl,
          rsEmailDomains,    <---- NEW ATTRIBUTE
          rsIdpPolicy,
          rsIdpPolicyFormat,
          rsIdpMetadata,
          enabled
  };

Repose Upgrade
---------------

Upgrade Repose to version 8.7.1.0 in order to support group assignment in v2 Federation

Deployment Notes
-----------------

---------------
Pre-Deployment
---------------

None.

-----------
Deployment
-----------

The Repose and IDM application must both be upgraded to support all the features of the IDM release.

---------------
Post-Deployment
---------------

None