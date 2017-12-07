package com.rackspacecloud.api.identity.v2

import com.rackspacecloud.Options
import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._
import io.gatling.core.feeder._
import com.typesafe.config.ConfigFactory

object TokensListUsersInDomain {
  val deploymentName = Options.GATLING_DEPLOYMENT_NAME

  val conf = ConfigFactory.load()
  
  var DATA_LOCATION : String = ""
  try{
    DATA_LOCATION = conf.getString("data_dir")
  } catch {
    case missing : Throwable => println("Missing data dir, using default")
  }
  val usersFeeder_v20_admin = csv(DATA_LOCATION + "data/identity/admin_users_tokens.dat").circular
  val usersInDomainFeeder = csv(DATA_LOCATION + "data/identity/users_in_domain.dat").circular
def v20_list_users_in_a_domain = {
      feed(usersFeeder_v20_admin)
        .feed(usersInDomainFeeder)
        .exec(http("GET /v2.0/RAX-AUTH/domains/${domainId}/users")
          .get("/v2.0/RAX-AUTH/domains/${domainId}/users")
          .header("x-auth-token", "${admin_token}")
          .header("X-Forwarded-For", "${ipaddress}")
          .check(status.is(200)))
        .exitHereIfFailed

}
}
