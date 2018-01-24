Cloud Identity 3.19.0 Release
==============================

.. _CID-356:  https://jira.rax.io/browse/CID-356
.. _CID-357:  https://jira.rax.io/browse/CID-357
.. _CID-1115:  https://jira.rax.io/browse/CID-1115
.. _CID-1308:  https://jira.rax.io/browse/CID-1308
.. _CID-503:  https://jira.rax.io/browse/CID-503
.. _CID-678:  https://jira.rax.io/browse/CID-678
.. _CID-1086:  https://jira.rax.io/browse/CID-1086
.. _CID-1312:  https://jira.rax.io/browse/CID-1312
.. _CID-1022:  https://jira.rax.io/browse/CID-1022

.. contents::

Info
----

Release Ticket  - Add JIRA link here.

.. csv-table:: Release Progress
  :header: Milestone, Date, Status

  RC Due, "Tues, Jan 30 2018",
  QE Signoff for Staging, "Thurs, Feb 1 2018",
  Staging Release Week, "Mon, Feb 5 - Tues, Feb 6 2018",
  Signoff for Production, "Fri, Feb 9 2018",
  Production Release Week, "Mon, Feb 12 - Tues, Feb 13 2018",


Significant Changes
-------------------


Issues Resolved
---------------

Stories
-------

#. `CID-1115`_ - Analyze token returns 404("Token not found.") if the user for the subject token is deleted
#. `CID-1308`_ - Implement healthCheck on ldap connection pools

Defects
-------

#. `CID-503`_ - Prevent endpoint template from being deleted if linked to endpoint assignment rule
#. `CID-678`_ - 'create otp device' results into 500 when device name is set to an empty string in the request
#. `CID-1086`_ - propagating field on user roles list is always false
#. `CID-1312`_ - User does not show up in the list if query param *roleId* is specified for the role added to user group on the tenant

Technical Debts
---------------

#. `CID-356`_ - Remove static properties 'feature.ignore.authentication.token.delete.failure.enabled' and 'feature.authentication.token.delete.failure.stops.cleanup.enabled'
#. `CID-357`_ - Remove old Foundation API static properties
#. `CID-1022`_ - Remove .gitmodules from cloud-identity project


Configuration Updates
---------------------

---
New
---
These are new properties added as part of the Release

.. csv-table:: Configuration Changes
   :header: "Name", "Description", "DefaultValue", "Story", "File"

   feature.enable.ldap.health.check.new.connection, "Whether to enable health check on new LDAP connection.", false,`CID-1308`_, reloadable
   feature.enable.ldap.health.check.connection.for.continued.use, "Whether to enable health check on valid connection for continued use.", false, `CID-1308`_, reloadable
   ldap.server.pool.min.disconnect.interval.time, "Specifies the minimum length of time in milliseconds that should pass between connections closed because they have been established for longer than the maximum connection age.", 0, `CID-1308`_, static

-------
Updates
-------
These properties are changes to the default settings for existing properties

.. csv-table:: Configuration Changes
   :header: "Name", "Description", "DefaultValue", "Story", "File"

-------
Deleted
-------

These properties should be removed from the respective properties files as they are no longer used.

.. csv-table:: Configuration Changes
   :header: "Name", "Story", "File"

   feature.ignore.authentication.token.delete.failure.enabled, `CID-356`_, static
   feature.authentication.token.delete.failure.stops.cleanup.enabled, `CID-356`_, static
   token.expirationSeconds, `CID-357`_, static
   token.rackerExpirationSeconds, `CID-357`_, static
   token.maxExpirationSeconds, `CID-357`_, static
   token.minExpirationSeconds, `CID-357`_, static
   token.refreshTokenExpirationSeconds, `CID-357`_, static

Directory Changes
------------------

---
New
---
<New Schema goes here>

-------
Updates
-------
<Updates to Schema goes here>

Repose Upgrade
--------------

Deployment Notes
----------------

--------------
Pre-Deployment
--------------

<Any pre-deployment steps go here>

-----------
Deployment
-----------

<Any notes for steps during deployment>

---------------
Pre-Deployment
---------------

<Any post-deployment steps go here>
