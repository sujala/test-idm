gatling-rax
===========

Gatling Simulations for Rackspace OpenStack Cloud

## Dependecies
Maven

## Running in intelliJ

* Open IntelliJ
* Import Project
* Select pom.xml
* Let intellij do its magic (pull down dependencies, etc.)
* Mark `performance/identity-perf-agent/src/test` directory as `Test Resources Root`
* In `performance/identity-perf-agent/src/test/scala/Engine.scala` set `props.SimulationClass` to whatever you want to run
* Right click and run `Engine`
