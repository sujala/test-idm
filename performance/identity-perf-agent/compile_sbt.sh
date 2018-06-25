#! /usr/bin/env bash
export SBT_OPTS="-Xmx768M -XX:+UseConcMarkSweepGC -XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=1G -Xss2M  -Duser.timezone=GMT"
sbt compile
