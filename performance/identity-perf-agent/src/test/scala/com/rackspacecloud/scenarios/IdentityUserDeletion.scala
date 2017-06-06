package com.rackspacecloud.scenarios

import com.rackspacecloud.api.identity.v2.UserDeletion

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.commons.validation._

import scala.util.Random
import scala.concurrent.duration._
import com.rackspacecloud.Options
import java.io._
import com.typesafe.config.ConfigFactory

object IdentityUserDeletion{

  val user_deletion = exec(UserDeletion.v20_user_deletion).exitHereIfFailed
  val user_delete = scenario("V2.0_User_Delete_External").exec(user_deletion)

}
