Cloud Identity 3.18.0 Release
==============================
.. _CID-1270:  https://jira.rax.io/browse/CID-1270
.. _CID-1271:  https://jira.rax.io/browse/CID-1271
.. _CID-1272:  https://jira.rax.io/browse/CID-1272
.. _CID-1281:  https://jira.rax.io/browse/CID-1281
.. _CID-1283:  https://jira.rax.io/browse/CID-1283
.. _CID-736:  https://jira.rax.io/browse/CID-736
.. _CID-1287:  https://jira.rax.io/browse/CID-1287
.. _CID-1297:  https://jira.rax.io/browse/CID-1297
.. _CID-1280:  https://jira.rax.io/browse/CID-1280
.. _CID-1230:  https://jira.rax.io/browse/CID-1230
.. _CID-1294:  https://jira.rax.io/browse/CID-1294
.. _CID-1238:  https://jira.rax.io/browse/CID-1238

.. contents::

Info
----

Release Ticket  - Add JIRA link here.

.. csv-table:: Release Progress
  :header: Milestone, Date, Status

  RC Due, "Thurs, Dec 21 2017", "Delivered - RC2 Build number: 3.18.0-1513809211095"
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

#. `CID-1270`_ - EPS: Update "List users for tenant" to support user groups
#. `CID-1271`_ - EPS: List "effective" roles for user service
#. `CID-1272`_ - EPS: Find user by Contact ID
#. `CID-1281`_ - Upgrade Repose to version 8.7.3.0
#. `CID-1283`_ -  Reclassifying some resources as unprotected to new relic
#. `CID-736`_ - Use subtree delete control when deleting entities
#. `CID-1280`_ -  Upgrade New Relic secured attributes to use sha256
#. `CID-1287`_ -  Enable password history by default
#. `CID-1297`_ -  Enable support for user groups globally within default docker container
#. `CID-1294`_ -  Revert Identity source compatibility to Java 7

Defects
-------
#. `CID-1230`_ - Using emailDomains query param in list IDP with invalid domainId returns 404
#. `CID-1238`_ - Updating email domains returns 500 if at least one of the email domain is an empty sting

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

   feature.use.subtree.delete.control.for.subtree.deletion.enabled,Whether to use subtree delete control for subtree deletion., true, `CID-736`_, reloadable
   feature.enable.new.relic.sha256.hmac,Whether or not to use SHA-256 for HMAC of New Relic attributes . If not\, uses SHA-1, true, `CID-1280`_, reloadable

-------
Updates
-------
These properties are changes to the default settings for existing properties

.. csv-table:: Configuration Changes
   :header: "Name", "Description", "DefaultValue", "Story", "File"

   feature.enforce.password.policy.history, Whether or not to enforce password policy history, true, `CID-1287`_, reloadable
   feature.maintain.password.history, Whether or not to maintain password history. If history enforcement is enabled, this is always true, true, `CID-1287`_, reloadable
   enable.user.groups.globally, Whether or not user groups are supported for all domains for management and considered during effective role calculation, true, `CID-1297`_, reloadable

-------
Deleted
-------

These properties should be removed from the respective properties files as they are no longer used.

.. csv-table:: Configuration Changes
   :header: "Name", "Story", "File"

   feature.use.subtree.delete.control.for.subtree.deletion.enabled, `CID-736`_, static
   auto.assign.role.on.domain.tenants.role.name,`CID-1271`_,reloadable

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
