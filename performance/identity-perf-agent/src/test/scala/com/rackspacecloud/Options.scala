package com.rackspacecloud

import io.gatling.jdbc.feeder.JdbcFeederSource

object Options {
  val GATLING_DEPLOYMENT_NAME = {
    val name =
      System.getProperty("rackspacecloud.options.gatling.deploymentName", "staging")
    if (!name.matches("^[a-zA-Z0-9]*$")) {
      throw new IllegalArgumentException("rackspacecloud.options.gatling.deploymentName" +
        " property must only contain alphanumeric values")
    }
    name
  }

  val GATLING_DB_JDBC_URL =
    System.getProperty("rackspacecloud.options.gatling.db.jdbcURL",
                       "jdbc:mysql://10.69.245.100/gatling")

  val GATLING_DB_USERNAME =
    System.getProperty("rackspacecloud.options.gatling.db.username", "root")

  val GATLING_DB_PASSWORD =
    System.getProperty("rackspacecloud.options.gatling.db.password", "cloudgazer")

  private def getAuthURL: String = {
//    val rows = JdbcFeederSource(GATLING_DB_JDBC_URL, GATLING_DB_USERNAME, GATLING_DB_PASSWORD,
//      s"""SELECT auth_url FROM deployment WHERE name = '$GATLING_DEPLOYMENT_NAME'""")
//    rows(0)("auth_url").asInstanceOf[String]
      "https://staging.identity.api.rackspacecloud.com"
  }

}
