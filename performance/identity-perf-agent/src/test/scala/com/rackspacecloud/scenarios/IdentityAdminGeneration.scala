package com.rackspacecloud.scenarios

import com.rackspacecloud.api.identity.v2.TokensAdminGeneration

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.commons.validation._

import scala.util.Random
import scala.concurrent.duration._
import com.rackspacecloud.Options
import java.io._
import com.typesafe.config.ConfigFactory

object IdentityAdminGeneration {
  val conf = ConfigFactory.load()
  var DATA_LOCATION : String = ""
  try{
    DATA_LOCATION = conf.getString("data_dir")
  } catch {
    case missing : Throwable=> println("Missing data dir, using default")
  }
 
  val admin_results = new PrintWriter(new File(DATA_LOCATION + "data/identity/admin_users_tokens.dat"))
  admin_results.append("admin_token,user_name,apikey,password,ipaddress\n")
  
  val v20_admin_generate_tokens = exec(TokensAdminGeneration.v20_admin_gen_tokens).exec {session => admin_results.write(
       session("token_id").as[String] + "," +
       session("user_name").as[String] + "," +
       session("apikey").as[String] + "," +
       session("password").as[String] + "," +
       session("ipaddress").as[String] +
       "\n"); session}
    .exitHereIfFailed
    .pause(5 seconds, 30 seconds)

  val generate_admin_tokens = scenario("Generate Admin Tokens").exec(v20_admin_generate_tokens)
}
