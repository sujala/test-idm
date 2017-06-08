package com.rackspacecloud.api.identity.v2

import com.rackspacecloud.Options
import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._
import io.gatling.core.feeder._
import com.typesafe.config.ConfigFactory

object UserDeletion {
  val conf = ConfigFactory.load()
  val deploymentName = Options.GATLING_DEPLOYMENT_NAME
  var DATA_LOCATION : String = ""
  try{
    DATA_LOCATION = conf.getString("data_dir")
  } catch {
    case missing : Throwable => println("Missing data dir, using default")
  }

  val createdUsersFeeder = csv(DATA_LOCATION + "data/identity/created_users.dat").random
  
def v20_user_deletion = {
    feed(createdUsersFeeder)
      .exec(http("DELETE /v2.0/users/{user_id}")
      .delete("/v2.0/users/${user_id}")
      .header("x-auth-token", "${admin_token}")
      .check(status.is(204)))
      .exitHereIfFailed

}
}


