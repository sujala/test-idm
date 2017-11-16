package com.rackspacecloud.simulations.identity

import com.rackspacecloud.Options
import com.rackspacecloud.scenarios.IdentityDefaultUserDeletion
import com.rackspacecloud.scenarios.IdentityUserDeletion

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import java.io._
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._


class IdentityConstantTputDefaultUserDeletions extends Simulation {
  val conf = ConfigFactory.load()
  val DELETE_USER_USERS_PER_SEC : Double =  conf.getDouble("soa.delete_user.users_per_sec")

  val DATA_GENERATION_PERIOD_SECS: Int = conf.getInt("generate_tokens.duration_secs")

  val OS_MAIN_EXTERNAL_AUTH_URL = conf.getString("main_external_auth_url")
  val httpConf = http
    .baseURL(OS_MAIN_EXTERNAL_AUTH_URL)
    .acceptHeader("application/json")
    .acceptEncodingHeader("gzip, deflate, compress")
    .contentTypeHeader("application/json; charset=utf-8")
    .userAgentHeader( "QEPerf/1.0.0")
    .shareConnections

  val default_user_delete_scn = IdentityDefaultUserDeletion.default_user_delete
    setUp(
    default_user_delete_scn
      .inject(constantUsersPerSec(DELETE_USER_USERS_PER_SEC) during(DATA_GENERATION_PERIOD_SECS seconds))
  ).protocols(httpConf).maxDuration(DATA_GENERATION_PERIOD_SECS seconds)


}
