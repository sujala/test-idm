package com.rackspacecloud.api.identity.v2

import com.rackspacecloud.Options
import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._
import io.gatling.core.feeder._
import com.typesafe.config.ConfigFactory

object DefaultUserDeletion {
  val conf = ConfigFactory.load()
  val deploymentName = Options.GATLING_DEPLOYMENT_NAME
  var DATA_LOCATION : String = ""
  try{
    DATA_LOCATION = conf.getString("data_dir")
  } catch {
    case missing : Throwable => println("Missing data dir, using default")
  }

  val usersFeeder_v20_admin = csv(DATA_LOCATION + "data/identity/admin_users_tokens.dat").circular
  val defaultUsersFeeder = csv(DATA_LOCATION + "data/identity/default_users.dat").circular

def v20_default_user_deletion: ChainBuilder = {
    feed(usersFeeder_v20_admin)
       .feed(defaultUsersFeeder)
      .exec(http("DELETE /v2.0/users/{user_id}")
        .delete("/v2.0/users/${userid}")
        .header("x-auth-token", "${admin_token}")
        .check(status.in(List(204, 404))))
      .exitHereIfFailed

  }
}


