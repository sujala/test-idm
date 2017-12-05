package com.rackspacecloud.simulations.identity

import com.rackspacecloud.scenarios.IdentityDomainUserDeletion

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._


class DomainUserDeletions extends Simulation {
  val conf = ConfigFactory.load()
  val DELETE_USER_USERS_PER_SEC : Double =  conf.getDouble("soa.delete_user.users_per_sec")

  val DELETION_PERIOD_SECS: Int = conf.getInt("soa.delete_domain_users.duration_secs")

  val OS_MAIN_EXTERNAL_AUTH_URL = conf.getString("main_external_auth_url")
  val httpConf = http
    .baseURL(OS_MAIN_EXTERNAL_AUTH_URL)
    .acceptHeader("application/json")
    .acceptEncodingHeader("gzip, deflate, compress")
    .contentTypeHeader("application/json; charset=utf-8")
    .userAgentHeader( "QEPerf/1.0.0")
   .shareConnections 

  val domain_user_delete_scn = IdentityDomainUserDeletion.domain_user_delete
  setUp(
    domain_user_delete_scn
      .inject(constantUsersPerSec(DELETE_USER_USERS_PER_SEC) during(DELETION_PERIOD_SECS seconds))
  ).protocols(httpConf).maxDuration(DELETION_PERIOD_SECS seconds)

}
