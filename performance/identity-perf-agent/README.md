gatling-rax
===========

Gatling Simulations for Rackspace OpenStack Cloud

## Dependecies
Maven (there is a build.sbt but that's outdated ... use maven instead)

## Running in intelliJ

* Open IntelliJ
* Import Project
* Select pom.xml
* Let intellij do its magic (pull down dependencies, etc.)
* Mark `src/test` directory as `Test Resources Root`
* In `src/test/scala/Engine.scala` set `props.SimulationClass` to whatever you want to run
* Right click and run `Engine`
