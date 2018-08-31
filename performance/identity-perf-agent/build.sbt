enablePlugins(GatlingPlugin)
import io.gatling.sbt.GatlingPlugin

name := "gatling-rax"

version := "0.0.2"

scalaVersion := "2.11.5"

scalacOptions := Seq(
  "-encoding", "UTF-8", "-target:jvm-1.8", "-deprecation",
  "-feature", "-unchecked", "-language:implicitConversions", "-language:postfixOps")

resolvers += "sonatype-releases" at "https://oss.sonatype.org/content/repositories/releases/"

//resolvers +=
//  "saml" at "https://staging.artifacts.rackspace.net/maven"
resolvers +=
  "idm" at "https://artifacts.rackspace.net/maven"


javaOptions in Gatling := overrideDefaultJavaOptions("-Xms512m", "-Xmx768m")
libraryDependencies += "io.gatling.highcharts" % "gatling-charts-highcharts" % "2.2.2" % "test,it"
libraryDependencies += "io.gatling"            % "gatling-test-framework"    % "2.2.2" % "test,it"
libraryDependencies += "mysql"                 % "mysql-connector-java"      % "5.1.34" % "test,it"
//libraryDependencies += "org.opensaml"          % "opensaml-core"             % "3.2.0"  % "test,it"
//libraryDependencies += "com.rackspace"         % "saml-generator"   % "2.1.0-1535599910628" % "test,it"
