package com.rackspacecloud.simulations.identity

import com.rackspacecloud.scenarios.Identity
import com.typesafe.config.ConfigFactory
import io.gatling.core.Predef._
import io.gatling.core.protocol.Protocol
import io.gatling.core.structure._
import io.gatling.http.Predef._

import scala.concurrent.duration._

class IdentityDemo extends Simulation {
  val conf = ConfigFactory.load()

  // V20 Authenticate
  val V20_AUTHENTICATE_APIKEY_USERS_PER_SEC : Double =  conf.getDouble("soa.v20_authenticate_apikey.users_per_sec")

  val v20_apikey_auth_scn = Identity.v20_apikey_auth


  // V20 Validate
  val V20_VALIDATE_USERS_PER_SEC : Double =  conf.getDouble("soa.v20_validate.users_per_sec")

  val v20_token_validate_scn = Identity.v20_token_validate


  // Not method or version specific.
  val MAX_DURATION_SECS: Int = conf.getInt("simulation.max_duration_secs")
  val OS_MAIN_EXTERNAL_AUTH_URL = conf.getString("main_external_auth_url")


  val httpMainExternalConf = http
    .baseURL(OS_MAIN_EXTERNAL_AUTH_URL)
    .acceptHeader("application/json")
    .acceptEncodingHeader("gzip, deflate, compress")
    .contentTypeHeader("application/json; charset=utf-8")
    .userAgentHeader( """QEPerf/1.0.0""")
    .shareConnections 


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
       scn_wrapper(v20_apikey_auth_scn, V20_AUTHENTICATE_APIKEY_USERS_PER_SEC, MAX_DURATION_SECS, httpMainExternalConf),
       scn_wrapper(v20_token_validate_scn, V20_VALIDATE_USERS_PER_SEC, MAX_DURATION_SECS, httpMainExternalConf)
     )
  }
  setUp(list_scns():_*
  ).maxDuration(MAX_DURATION_SECS seconds)

 after {
   Identity.created_users_writer.flush()
   Identity.created_users_writer.close()
 }

}
