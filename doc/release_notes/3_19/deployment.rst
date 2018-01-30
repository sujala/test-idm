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
.. _CID-1286:  https://jira.rax.io/browse/CID-1286
.. _CID-1289:  https://jira.rax.io/browse/CID-1289
.. _CID-1290:  https://jira.rax.io/browse/CID-1290
.. _CID-1324:  https://jira.rax.io/browse/CID-1324
.. _CID-1326:  https://jira.rax.io/browse/CID-1326

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
#. `CID-1286`_ - apply_rcn_roles functionality should only auto assign global roles to non-excluded tenants
#. `CID-1289`_ - Reduce performance impact of locked users
#. `CID-1290`_ - Revise New Relic reporting with annotations
#. `CID-1324`_ - Allow a contactId to be set on a federated user
#. `CID-1326`_ - Return contactId for federated users from get/list users and validate token

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
   feature.enable.ldap.auth.password.lockout.cache,"",true,`CID-1289`_, reloadable
   ldap.auth.password.lockout.duration,"Duration of lockout period",PT1S,`CID-1289`_, reloadable
   ldap.auth.password.lockout.retries,"Number of failed pwd auth attempts before lockout",6,`CID-1289`_, reloadable
   ldap.auth.password.lockout.cache.ttl,"The TTL of entries in the lockout cache. Should be greater than lockout duration",PT1M,`CID-1289`_, static
   ldap.auth.password.lockout.cache.size,"Number of entries to store in the lockout cache.",200,`CID-1289`_, static
   new.relic.include.auth.resource.attributes,"The custom attributes to send to New Relic for Auth requests. '\*'' means all",\*,`CID-1290`_, reloadable
   new.relic.exclude.auth.resource.attributes,"The custom attributes to exclude from sending to New Relic for Auth requests. An attribute in both exclude and include lists will be excluded.",,`CID-1290`_, reloadable
   new.relic.include.private.resource.attributes,"The custom attributes to send to New Relic for private requests. '\*'' means all",\*,`CID-1290`_, reloadable
   new.relic.exclude.private.resource.attributes,"The custom attributes to exclude from sending to New Relic for private requests. An attribute in both exclude and include lists will be excluded.",,`CID-1290`_, reloadable
   new.relic.include.public.resource.attributes,"The custom attributes to send to New Relic for public requests. '\*'' means all",\*,`CID-1290`_, reloadable
   new.relic.exclude.public.resource.attributes,"The custom attributes to exclude from sending to New Relic for public requests. An attribute in both exclude and include lists will be excluded.",,`CID-1290`_, reloadable

-------
Updates
-------
These properties are changes to the default settings for existing properties

.. csv-table:: Configuration Changes
   :header: "Name", "Description", "DefaultValue", "Story", "File"

   new.relic.secured.api.resource.attributes,"The attributes to secure","callerToken,effectiveCallerToken,callerUsername,effectiveCallerUsername,callerUserType,effectiveCallerUserType",`CID_1290`_, reloadable

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
   ldap.password.failure.lockout.min,`CID-1289`_, static
   feature.enable.secure.new.relic.api.resource.attributes,`CID-1290`_,reloadable
   new.relic.auth.api.resource.attributes,`CID-1290`_,reloadable
   new.relic.protected.api.resource.attributes,`CID-1290`_,reloadable
   new.relic.unprotected.api.resource.attributes,`CID-1290`_,reloadable   

Directory Changes
------------------

---
New
---
<New Schema goes here>

-------
Updates
-------

1. Add ``rsContactId`` to ``rsFederatedPerson`` under ``may-contain``.

.. code::

    schema set object-class (1.3.6.1.4.1.20988.2.2.37) = {
        name = rsFederatedPerson
        ldap-names = rsFederatedPerson
        subclass-of top
        kind = structural
        must-contain
            rsId,
            uid,
            rsRegion,
            rsDomainId,
            mail,
            labeledUri
        may-contain
            rsGroupId,
            rsContactId,
            rsUserGroupDNs,
            rsFederatedUserExpiredTimestamp
    };

Repose Upgrade
--------------

Deployment Notes
----------------

A number of changes were made in how to control which attributes get sent to New
Relic along with the property names that will require changes to staging/prod. 

1. Previously one must explicitly specify
which attributes to include if at least one available attributes should not be
sent. This release adds the capability to only needing to specify those
attributes that should not be sent.

Staging/Prod are currently configured to **not** send ``callerUserType``
and ``effectiveCallerUserType`` as this is sensitive data w/ a small number of 
unique values. They are excluded by listing all the attributes that should be sent (
and leaving these out) in the
``new.relic.protected.api.resource.attributes`` configuration. With 3.19.0 this
is much simpler can be be configured via:

..

  new.relic.include.private.resource.attributes=\*
  new.relic.exclude.private.resource.attributes=callerUserType,effectiveCallerUserType

2. As part of the changes token attributes are no longer automatically masked. 
Instead they must be secured like any other sensitive attribute. Therefore the
current setting for ``new.relic.secured.api.resource.attributes`` must be updated
to include ``callerToken`` and ``effectiveCallerToken``. The config would be:

..

  new.relic.secured.api.resource.attributes=callerToken,effectiveCallerToken,callerUsername,effectiveCallerUsername,callerUserType,effectiveCallerUserType

--------------
Pre-Deployment
--------------

-----------
Deployment
-----------

<Any notes for steps during deployment>

---------------
Post-Deployment
---------------

<Any post-deployment steps go here>
