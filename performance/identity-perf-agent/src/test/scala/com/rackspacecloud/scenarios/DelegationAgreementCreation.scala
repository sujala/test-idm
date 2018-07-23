package com.rackspacecloud.scenarios

import com.rackspacecloud.api.identity.v2.Tokens

import io.gatling.core.Predef._

import scala.concurrent.duration._
import java.io._
import com.typesafe.config.ConfigFactory

object DelegationAgreementCreation {
  val conf = ConfigFactory.load()
  var DATA_LOCATION : String = ""
  try{
    DATA_LOCATION = conf.getString("data_dir")
  } catch {
    case missing : Throwable=> println("Missing data dir, using default")
  }
 
  val create_parent_das = scenario("Create Delegation Agreements").exec(Tokens.v20_create_delegation_agreement)
}
