Cloud Identity 3.18.0 Release
==============================
.. _CID-1281:  https://jira.rax.io/browse/CID-1281
.. _CID-1283:  https://jira.rax.io/browse/CID-1283
.. _CID-1287:  https://jira.rax.io/browse/CID-1287
.. contents::

Info
----

Release Ticket  - Add JIRA link here.

.. csv-table:: Release Progress
   :header: Milestone, Date, Status

   RC Due, "Thurs, Dec 14 2017", In Progress.
   QE Signoff for Staging, "Thurs, Jan 4 2017",
   Staging Release Week, "Mon, Jan 8 - Tues, Jan 9 2018",
   Signoff for Production, "Fri, Jan 12 2018",
   Production Release Week, "Mon, Jan 15 - Tues, Jan 16 2018",


Significant Changes
-------------------


Issues Resolved
---------------

Stories
-------

#. `CID-1281`_ - Upgrade Repose to version 8.7.3.0
#. `CID-1283`_ -  Reclassifying some resources as unprotected to new relic
#. `CID-1287`_ -  Enable password history by default

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

   None,

-------
Updates
-------
These properties are changes to the default settings for existing properties 

.. csv-table:: Configuration Changes
   :header: "Name", "Description", "DefaultValue", "Story", "File"

   feature.enforce.password.policy.history, 'Whether or not to enforce password policy history', true, CID-1287, reloadable
   feature.maintain.password.history, 'Whether or not to maintain password history. If history enforcement is enabled, this is always true', true, CID-1287, reloadable


-------
Deleted
-------

These properties should be removed from the respective properties files as they are no longer used.

.. csv-table:: Configuration Changes
   :header: "Name", "Story", "File"

   None,

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

Upgrade Repose to version 8.7.3.0.

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
