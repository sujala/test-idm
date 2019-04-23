package com.rackspacecloud.simulations.identity

import com.rackspacecloud.scenarios.Identity
import com.rackspacecloud.scenarios.IdentityChangePasswordOfUserAcc
import com.typesafe.config.ConfigFactory
import io.gatling.core.Predef._
import io.gatling.core.protocol.Protocol
import io.gatling.core.structure._
import io.gatling.http.Predef._

import scala.concurrent.duration._

class ChangePassword extends Simulation {
  val conf = ConfigFactory.load()

  // V20 List users for domain
  val V20_CHANGE_PASSWORD_USERS_PER_SEC : Double =  conf.getDouble("soa.v20_change_password.users_per_sec")

  val v20_users_for_change_password = IdentityChangePasswordOfUserAcc.v20_change_password

  // Not method or version specific.
  val MAX_DURATION_SECS: Int = conf.getInt("simulation.max_duration_secs_for_change_password")
  val OS_MAIN_EXTERNAL_AUTH_URL = conf.getString("main_external_auth_url")


  val httpMainExternalConf = http
    .baseURL(OS_MAIN_EXTERNAL_AUTH_URL)
    .acceptHeader("application/json")
    .acceptEncodingHeader("gzip, deflate, compress")
    .contentTypeHeader("application/json; charset=utf-8")
    .userAgentHeader( """QEPerf/1.0.0""")
    .shareConnections


  def scn_wrapper(scenario: ScenarioBuilder, calls_per_sec: Double, duration: Int, protocol_conf: Protocol): PopulationBuilder= {
    var local_users_per_sec = calls_per_sec;
    var local_duration = duration;
    scenario.inject(constantUsersPerSec(local_users_per_sec) during(local_duration seconds)).protocols(protocol_conf)
  }
  def list_scns(): List[PopulationBuilder] = {
    return List(
      scn_wrapper(v20_users_for_change_password, V20_CHANGE_PASSWORD_USERS_PER_SEC, MAX_DURATION_SECS, httpMainExternalConf)
    )
  }
  setUp(list_scns():_*
  ).maxDuration(MAX_DURATION_SECS seconds)

  after {
    Identity.created_users_writer.flush()
    Identity.created_users_writer.close()
  }

}
