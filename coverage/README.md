Code generation with Jacoco
===========================

Background
----------
Jacoco is a library that can be used to generate both a code coverage database,
and html reports from a code coverage database. In order to support integration
and system tests which must run outside of a JVM, a set of scripts were 
written that allow for setup, starting, stopping (generating a code coverage
database) and removing code coverage.

Assumptions
-----------
We are running tests against identity set up as a set of docker instances.

Approach
--------
There are two ways to instrument a set of java classes with Jacoco. One is
to generate a set of instrumented classes. This is the approach normally taken
when it is run within Maven or Groovy. The second is by instrumenting 
"on the fly", in which Jacoco uses a hook provided by Java. 

See [Jacoco Design](http://www.eclemma.org/jacoco/trunk/doc/implementation.html)

In our case we setup Catalina to include a pointer to the Jacoco agent jar, and
the name of a file to write the code coverage database to.

We get coverage results for a run by:

1. stopping tomcat
1. (if needed) Setting up coverage 
1. removing any present code coverage database
1. Starting tomcat which will cause a new code coverage database to start
1. Interact with the app (could be manual tests, could be automated.)
1. Stop tomcat
1. Fetch the code coverage database
1. Fetch the identity jar (to get class information)
1. Run gradle to generate a report 

There is also a script to wipe code coverage from a setup docker.

Scripts And Files
-----------------
Conventions
===========
I used *_coverage.sh for scripts that are run outside the docker. I 
used *_jacoco.sh for scripts that get run inside the docker. I used positional
parameters for all of the scripts, I donate them with the bash convention of
$1, $2, etc.
For the descriptions, I'm assuming that a user will use the *_coverage.sh
scripts, so I only put pre and post conditions for those.

start_coverage.sh
=================
This script copies the setup_jacoco.sh file over and calls setup_jacoco.sh in
the context of the docker. It also copies over the start and stop tomcat
scripts.

### Parameters
* $1 - This the docker identity istance we want to cover. It currently only
works for identity.
* $2 - Jacoco version. Defaults to 0.7.5.201505241946.

### Pre-conditions
* Setup Identity docker instance

### Post-conditions
* Identity is running with on the fly instrumentation. A jacoco.exec file should
be present in /tmp.
* A copy of the Jacoco setup script will be on the machine in /etc/idm.
* A copy of the Jacoco code will have been copied to /etc/idm/jacoco/lib
* /usr/share/tomcat7/bin/setenv.sh will have been updated to add the Jacoco
agent

setup_jacoco.sh
===============
This script downloads Jacoco and updates the Catalina setup script so that
the application is instrumented on-the-fly.

### Parameters
* $1 - Jacoco version. Defaults to 0.7.5.201505241946.


### Steps
1. Download the requested version of Jacoco.
1. (If needed) Update the Catalina setup script.
1. Stop tomcat (It current polls 10 times with 10 sec breaks).
1. Check that /tmp/jacoco.exec is present.

stop_coverage.sh
================
This script copies stop_jacoco.sh over to the docker and runs it in the context
of the docker instance. It then copies the code database out of the docker.
### Parameters
* $1 - Identity docker instance name.
* $2 - This is the version of identity as shown on the jar, for example: 
3.1.1-1452548987106 in identity-3.1.1-1452548987106.jar.

### Pre-conditions
* Jacoco setup on target docker.

### Post-conditions
* The code coverage db will have been copied locally to target/jacoco.exec
* Not that this does not actually stop collection coverage, instead it creates
a snapshot of of coverage by stopping tomcat and grabbing the database. So 
think of stop as in the test run has stopped.

stop_jacoco.sh
==============
This script stops Tomcat, renames the previous coverage db so it doesn't get 
overwritten and restarts Tomcat.

### Parameters
* $1 - Jacoco version. Defaults to 0.7.5.201505241946.


### Steps
1. Stop tomcat
1. remove previous temp file (/tmp/jacoco_current.exec)
1. Copy /tmp/jacoco.exec to temp file /tmp/jacoco_current.exec. This allows
us to restart tomcat without affecting the code coverage database. The 
stop_coverage script will rename it to jacoco.exec when it copies it out.
1. Restart tomcat (which will create a new jacoco.exec)

making a report
===============
Not really a script, but here are instructions on how to generate a report
after you've called stop_coverage.

Run

gradle applicationCodeCoverageReport

in the main cloud-identity directory.

### Pre-conditions
* You need gradle installed according to the directions on the dev environment
  setup website

### Postconditions
There will be a report at:

build/customJacocoReportDir/applicationCodeCoverageReport/html/index.html


remove_coverage.sh
==================
Copies over the remove_jacoco.sh script and runs it in the docker.

### Parameters
* $1 - Name of identity docker instance
### Pre-conditions
* None
### Post-conditions
* Code coverage (including updates to the Catelina startup) has been 
removed.

remove_jacoco.sh
================
Removes the Jacoco references in the Catalina setup file. Stops and starts
Tomcat afterwards.

### Parameters
* None

### Steps
1. Remove any lines with JACOCO from the Catalina setup.
1. remove /etc/idm/jacoco
1. restart Tomcat

start_tomcat.sh
===============

### Parameters
* None

### Steps
1. Calls /etc/init.d/tomcat7 stop
1. In a loop with a 10 second delay, greps ps aux for any catalina references
1. If, after 10 tries (100 seconds), catalina doesn't start,  we will fail and
   return -1
1. Otherwise, we just return.

stop_tomcat.sh
===============

### Parameters
* None

### Steps
1. Calls /etc/init.d/tomcat7 start
1. In a loop with a 10 second delay, greps ps aux for any catalina references
1. If, after 10 tries (100 seconds), catalina is still running,  we will fail
   and return -1
1. Otherwise, we just return.


To Do List
----------
* Jacoco has the ability to connect with a socket instead, according to it's
documentation. We would still need to do the setup, but we should be able to
create a coverage db from outside the docker instead. 
