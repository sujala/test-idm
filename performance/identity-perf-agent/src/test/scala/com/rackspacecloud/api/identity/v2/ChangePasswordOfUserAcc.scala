package com.rackspacecloud.api.identity.v2

import com.rackspacecloud.Options
import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._
import io.gatling.core.feeder._
import com.typesafe.config.ConfigFactory

object ChangePasswordOfUserAcc {
  val deploymentName = Options.GATLING_DEPLOYMENT_NAME
  val conf = ConfigFactory.load()
  var DATA_LOCATION : String = ""
  try{
    DATA_LOCATION = conf.getString("data_dir")
  } catch {
    case missing : Throwable => println("Missing data dir, using default")
  }

  def change_password_v20_default_user = csv(DATA_LOCATION + "data/identity/default_users.dat").circular

  // Change Password API is called for Users
  // Password Blacklist service is used to test if provided new password is compromised
  // HTTP Status code=400 used here is to verify if API makes use of Password Blacklist service
  // and not to test the end-to-end flow
  def v20_change_password = {
    feed(ChangePasswordOfUserAcc.change_password_v20_default_user)
      .exec(http("POST /v2.0/users/RAX-AUTH/change-pwd")
      .post("/v2.0/users/RAX-AUTH/change-pwd")
      .body(ElFileBody("request-bodies/identity/v2/change_password_v2.json")).asJSON
      .check(status.is(400)))
      .exitHereIfFailed

  }
}


