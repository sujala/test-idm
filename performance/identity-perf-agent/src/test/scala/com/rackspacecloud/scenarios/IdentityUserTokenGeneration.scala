package com.rackspacecloud.scenarios

import com.rackspacecloud.api.identity.v2.TokensUserGeneration

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.commons.validation._

import scala.util.Random
import scala.concurrent.duration._
import com.rackspacecloud.Options
import java.io._
import com.typesafe.config.ConfigFactory

object IdentityUserTokenGeneration {
   val conf = ConfigFactory.load()
  var DATA_LOCATION : String = ""
  try{
    DATA_LOCATION = conf.getString("data_dir")
  } catch {
    case missing : Throwable=> println("Missing data dir, using default")
  }
 
  val results = new PrintWriter(new File(DATA_LOCATION + "data/identity/users_tokens.dat"))
  
  val user_generate_tokens = exec(TokensUserGeneration.v20_user_gen_tokens).exec {session => results.write(
       session("token_id").as[String] + "," + 
       session("username").as[String] + "," + 
       session("apikey").as[String] + "," + 
       session("ipaddress").as[String] + 
       "\n"); session}
    .exitHereIfFailed
    .pause(5 seconds, 30 seconds)

  val generate_tokens = scenario("Generate User Tokens").exec(user_generate_tokens)

}
