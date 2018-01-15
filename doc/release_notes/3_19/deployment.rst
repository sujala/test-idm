Cloud Identity 3.19.0 Release
==============================

.. _CID-356:  https://jira.rax.io/browse/CID-356
.. _CID-357:  https://jira.rax.io/browse/CID-357

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

Defects
-------

Technical Debts
---------------

#. `CID-356`_ - Remove static properties 'feature.ignore.authentication.token.delete.failure.enabled' and 'feature.authentication.token.delete.failure.stops.cleanup.enabled'
#. `CID-357`_ - Remove old Foundation API static properties


Configuration Updates
---------------------

---
New
---
These are new properties added as part of the Release

.. csv-table:: Configuration Changes
   :header: "Name", "Description", "DefaultValue", "Story", "File"

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
