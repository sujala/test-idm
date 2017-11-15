Cloud Identity 3.17.1 Release Notes
===================================
.. _CID-1251:  https://jira.rax.io/browse/CID-1251
.. _CID-1252:  https://jira.rax.io/browse/CID-1252
.. _CID-1231:  https://jira.rax.io/browse/CID-1231
.. _CID-1254:  https://jira.rax.io/browse/CID-1254
.. _CID-1239:  https://jira.rax.io/browse/CID-1239
.. _CIDM-963:  https://jira.rax.io/browse/CIDM-963
.. contents::

Info
------

Release Ticket - `CID-1254`_

.. csv-table:: Release Progress
   :header: Milestone, Date, Status

   RC Due, "Tues, Nov 14 2017", Delivered - RC1 Build number: 3.17.1-1510607797053
   QE Signoff for Staging, "Thurs, Nov 16 2017",
   Staging Release, "Mon, Nov 27 - Tues, Nov 28 2017",
   Signoff for Production, "Fri, Dec 1 2017",
   Production Release Week, "Mon, Dec 4 - Tues, Dec 5 2017"

Significant Changes
--------------------
NewRelic enhancements.

Issues Resolved
----------------

-------
Stories
-------

#. `CID-1231`_ -  Add custom attributes to new relic logging
#. `CID-1251`_ -  Use the Repose X-Request-Id header as transaction id
#. `CID-1252`_ -  Support securing New Relic custom attributes

--------
Defects
--------

#. `CID-1239`_ - User-default does not properly remove all `tenant-access` roles from roles list on validate call


Configuration Updates
----------------------

----
New
----
These properties were added as part of this release.

.. list-table:: Configuration Changes
   :header-rows: 1
   :widths: 8 60 7 7 7

   * - Name
     - Description
     - DefaultValue
     - Story
     - File
   * - feature.enable.send.new.relic.custom.data
     - Whether or not to push custom attributes to New Relic for each API transaction
     - true
     - `CID-1231`_
     - reloadable
   * - new.relic.auth.api.resource.attributes
     - The custom attributes to push for auth api resources. '*' means all available
     - \*
     - `CID-1231`_
     - reloadable
   * - new.relic.protected.api.resource.attributes
     - The custom attributes to push for protected api resources. '*' means all available
     - \*
     - `CID-1231`_
     - reloadable
   * - new.relic.unprotected.api.resource.attributes
     - The custom attributes to push for unprotected api resources. '*' means all available
     - \*
     - `CID-1231`_
     - reloadable
   * - feature.enable.secure.new.relic.api.resource.attributes
     - Only relevant if sending new relic data is enabled. This controls whether or not to secure a specified set of attributes sent to new relic.
     - true
     - `CID-1252`_
     - reloadable
   * - relic.secured.api.resource.key
     - When secure attributes are enabled, the key to use for securing the props
     - 
     - `CID-1252`_
     - reloadable
   * - new.relic.secured.api.resource.attributes
     - When secure attributes are enabled, a comma delimited list to secure
     - callerUsername
       ,effectiveCallerUsername
       ,callerUserType
       ,effectiveCallerUserType
     - `CID-1252`_
     - reloadable
   * - feature.enable.use.repose.request.id
     - Whether or not to use the value supplied in the X-Request-Id header as the log transaction id. If set to false (or set to true but the header is null or blank), Identity generates a GUUID for the transaction id.
     - true
     - `CID-1251`_
     - reloadable

-------
Updates
-------
These properties were added as part of a previous release, but this release made changes such that they are expected to be updated.

None

.. csv-table:: Configuration Changes
   :header: "Name", "Description", "DefaultValue", "Story", "File"

-------
Deleted
-------

These properties should be removed from the respective properties files as they are no longer used.

None

.. csv-table:: Deleted Configurations
   :header: "Name", "Story", "File"


Directory Changes
------------------

----
New
----

None

--------
Updates
--------

None

Repose Changes
---------------

--------------
Config Changes
--------------

system-model.cfg.xml change shown below ::

	Index: repose/config/system-model.cfg.xml
	===================================================================
	--- repose/config/system-model.cfg.xml	(date 1503361856000)
	+++ repose/config/system-model.cfg.xml	(date 1510291146000)
	@@ -35,4 +35,5 @@
	       <endpoint default="true" hostname="172.17.0.1" id="identity" port="8083" protocol="http" root-path="" />
	     </destinations>
	   </repose-cluster>
	+  <tracing-header secondary-plain-text="true"/>
	 </system-model>


Deployment Notes
-----------------
Prior to being deployed `CID-1254`_ must be implemented to add Repose puppet configuration support for the 'X-Request-Id' header.

---------------
Pre-Deployment
---------------

None.

-----------
Deployment
-----------

None

---------------
Post-Deployment
---------------

None