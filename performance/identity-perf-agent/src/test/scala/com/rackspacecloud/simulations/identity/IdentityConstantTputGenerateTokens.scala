package com.rackspacecloud.simulations.identity

import com.rackspacecloud.Options
import com.rackspacecloud.scenarios.{Identity, IdentityAdminGeneration, IdentityDefaultUserTokenGeneration, IdentityUserTokenGeneration}
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import java.io._

import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._


class IdentityConstantTputGenerateTokens extends Simulation {
  val conf = ConfigFactory.load()
  val ADMIN_USERS_PER_SEC : Double =  conf.getDouble("soa.generate_admin_tokens.users_per_sec")

  val VALIDATE_USERS_PER_SEC : Double =  conf.getDouble("soa.generate_user_tokens.users_per_sec")
  val DATA_GENERATION_PERIOD_SECS: Int = conf.getInt("generate_tokens.duration_secs")

  val OS_MAIN_EXTERNAL_AUTH_URL = conf.getString("main_external_auth_url")
  val httpConf = http
    .baseURL(OS_MAIN_EXTERNAL_AUTH_URL)
    .acceptHeader("application/json")
    .acceptEncodingHeader("gzip, deflate, compress")
    .contentTypeHeader("application/json; charset=utf-8")
    .userAgentHeader( "QEPerf/1.0.0")
   .shareConnections

  val admin_gen_scn = IdentityAdminGeneration.generate_admin_tokens
  val valid_gen_scn = IdentityUserTokenGeneration.generate_tokens
  val default_gen_scn = IdentityDefaultUserTokenGeneration.generate_tokens
  setUp(
    admin_gen_scn
      .inject(constantUsersPerSec(ADMIN_USERS_PER_SEC) during(DATA_GENERATION_PERIOD_SECS seconds)),
    valid_gen_scn
      .inject(constantUsersPerSec(VALIDATE_USERS_PER_SEC) during(DATA_GENERATION_PERIOD_SECS seconds)),
    default_gen_scn
    .inject(constantUsersPerSec(VALIDATE_USERS_PER_SEC) during(DATA_GENERATION_PERIOD_SECS seconds))
  ).protocols(httpConf).maxDuration(DATA_GENERATION_PERIOD_SECS seconds)
    after {

    IdentityAdminGeneration.admin_results.flush()
    IdentityAdminGeneration.admin_results.close()
      IdentityUserTokenGeneration.results.flush()
      IdentityUserTokenGeneration.results.close()
      IdentityDefaultUserTokenGeneration.results.flush()
      IdentityDefaultUserTokenGeneration.results.close()
      Identity.created_users_writer.flush()
      Identity.created_users_writer.close()

}
}
