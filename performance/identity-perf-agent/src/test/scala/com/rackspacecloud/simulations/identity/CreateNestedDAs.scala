package com.rackspacecloud.simulations.identity

import com.rackspacecloud.scenarios.CreateNestedDAs
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import com.typesafe.config.ConfigFactory
import io.gatling.core.protocol.Protocol
import io.gatling.core.structure.{PopulationBuilder, ScenarioBuilder}

import scala.concurrent.duration._


class CreateNestedDAs extends Simulation {
  val conf = ConfigFactory.load()
  val DAS_PER_SEC : Double =  conf.getDouble("soa.parent_das_per_sec")

  val DATA_GENERATION_PERIOD_SECS: Int = conf.getInt("create_das.duration_secs")
  val MAX_DURATION_SECS: Int = conf.getInt("simulation.max_duration_secs")

  val OS_MAIN_EXTERNAL_AUTH_URL = conf.getString("main_external_auth_url")
  val httpConf = http
    .baseURL(OS_MAIN_EXTERNAL_AUTH_URL)
    .acceptHeader("application/json")
    .acceptEncodingHeader("gzip, deflate, compress")
    .contentTypeHeader("application/json; charset=utf-8")
    .userAgentHeader( "QEPerf/1.0.0")
   .shareConnections

  val create_nested_da_scn = CreateNestedDAs.create_nested_das

  def scn_wrapper(scenario: ScenarioBuilder, users_per_sec: Double, duration: Int, protocol_conf: Protocol): PopulationBuilder= {
    var local_users_per_sec = users_per_sec;
    var local_duration = duration;
    if (users_per_sec <= 0.05){
      local_users_per_sec = 1;
      local_duration = 1;
    }
    scenario.inject(constantUsersPerSec(local_users_per_sec) during(local_duration seconds)).protocols(protocol_conf)
  }
  def list_scns(): List[PopulationBuilder] = {
    return List(
      scn_wrapper(create_nested_da_scn, DAS_PER_SEC, MAX_DURATION_SECS, httpConf)
    )
  }
  setUp(list_scns():_*
  ).maxDuration(MAX_DURATION_SECS seconds)

}
