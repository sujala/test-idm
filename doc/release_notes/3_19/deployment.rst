Cloud Identity 3.19.0 Release
==============================

.. _CID-357:  https://jira.rax.io/browse/CID-357
.. _CID-1308:  https://jira.rax.io/browse/CID-1308

.. contents::

Info
----

Release Ticket  - Add JIRA link here.

.. csv-table:: Release Progress
  :header: Milestone, Date, Status

  RC Due, "",
  QE Signoff for Staging, "",
  Staging Release Week, "",
  Signoff for Production, "",
  Production Release Week, "",


Significant Changes
-------------------


Issues Resolved
---------------

Stories
-------

#. `CID-357`_ - Remove old Foundation API static properties
#. `CID-1308`_ - Implement healthCheck on ldap connection pools

Defects
-------

Technical Debts
---------------


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
