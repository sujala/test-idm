package com.rackspacecloud.api.identity.v2

import com.rackspacecloud.Options
import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._
import io.gatling.core.feeder._
import com.typesafe.config.ConfigFactory

object TokensAdminGeneration {
  val deploymentName = Options.GATLING_DEPLOYMENT_NAME
  val conf = ConfigFactory.load()
  var DATA_LOCATION : String = ""
  try{
    DATA_LOCATION = conf.getString("data_dir")
  } catch {
    case missing : Throwable => println("Missing data dir, using default")
  }

  def get_usersFeeder_v20_admin_gen = csv(DATA_LOCATION + "data/identity/admin_users.dat").circular
def v20_admin_gen_tokens = {
    feed(TokensAdminGeneration.get_usersFeeder_v20_admin_gen)
      .exec(http("POST /v2.0/tokens")
      .post("/v2.0/tokens")
      .header("X-Forwarded-For", "${ipaddress}")
      .body(ElFileBody("request-bodies/identity/v2/admin_tokens_body_v2.json")).asJSON
      .check(status.is(200))
      .check(jsonPath("$.access.token.id").saveAs("token_id")))
      .exitHereIfFailed

}
}


