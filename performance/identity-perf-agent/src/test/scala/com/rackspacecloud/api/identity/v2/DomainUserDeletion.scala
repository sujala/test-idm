package com.rackspacecloud.api.identity.v2

import com.rackspacecloud.Options
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import com.typesafe.config.ConfigFactory

object DomainUserDeletion {
  val conf = ConfigFactory.load()
  val deploymentName = Options.GATLING_DEPLOYMENT_NAME
  var DATA_LOCATION : String = ""
  try{
    DATA_LOCATION = conf.getString("data_dir")
  } catch {
    case missing : Throwable => println("Missing data dir, using default")
  }

  val createdUsersFeeder = csv(DATA_LOCATION + "data/identity/users_in_domain.dat").random
  val usersFeeder_v20_admin = csv(DATA_LOCATION + "data/identity/admin_users_tokens.dat").circular

def v20_user_deletion = {
    feed(createdUsersFeeder)
      .feed(usersFeeder_v20_admin)
      .exec(http("DELETE /v2.0/users/{userid}")
      .delete("/v2.0/users/${userid}")
      .header("x-auth-token", "${admin_token}")
      .check(status.in(List(204, 404))))
      .exitHereIfFailed

}
}


