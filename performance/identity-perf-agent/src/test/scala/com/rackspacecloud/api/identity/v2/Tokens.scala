package com.rackspacecloud.api.identity.v2

import com.rackspacecloud.Options
import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import com.typesafe.config.ConfigFactory
import io.gatling.http.Predef._
import java.io._
import scala.{Console => ScalaConsole}
//import com.rackspace.idm.federation.samlgenerator.FederatedAuthV2
import sys.process._
import org.opensaml.core.config.InitializationService
import collection.JavaConversions._
import com.rackspace.idm.federation.samlgenerator.v2.FederatedDomainAuthRequestGenerator
import com.rackspace.idm.federation.samlgenerator.v2.ResponseFlavor
import com.rackspace.idm.federation.samlgenerator.v2.FederatedDomainAuthGenerationRequest
import org.joda.time.DateTime
import com.rackspace.idm.federation.utils.SamlCredentialUtils
import java.util.UUID

object Tokens {
  val conf = ConfigFactory.load()
  val deploymentName = Options.GATLING_DEPLOYMENT_NAME
  
  var DATA_LOCATION : String = ""
  try{
    DATA_LOCATION = conf.getString("data_dir")
  } catch {
    case missing : Throwable=> println("Missing data dir, using default")
  }
  // Initializes underlying xml engine for the saml generator.
  // You'll get a null pointer exception in that engine's marshalling code if it's not here.
  InitializationService.initialize() 
  val DEFAULT_BROKER_PRIVATE_KEY = "sample_keys/fed-broker.pkcs8"
  val DEFAULT_BROKER_PUBLIC_KEY = "sample_keys/fed-broker.crt"
  val DEFAULT_ORIGIN_PRIVATE_KEY = "sample_keys/fed-origin.pkcs8"
  val DEFAULT_ORIGIN_PUBLIC_KEY = "sample_keys/fed-origin.crt"
  
  
  
  val brokerPrivateKey = DEFAULT_BROKER_PRIVATE_KEY
  val brokerPublicKey = DEFAULT_BROKER_PUBLIC_KEY
  val originPrivateKey = DEFAULT_ORIGIN_PRIVATE_KEY
  val originPublicKey = DEFAULT_ORIGIN_PUBLIC_KEY
  
  val brokerCredential = SamlCredentialUtils.getSigningCredential(brokerPublicKey, brokerPrivateKey)
  val originCredential = SamlCredentialUtils.getSigningCredential(originPublicKey, originPrivateKey)
  val generator = new FederatedDomainAuthRequestGenerator(brokerCredential, originCredential)

  val usersFeeder = csv(DATA_LOCATION + "data/identity/users_tokens.dat").circular
  val usersFeeder_v20_admin = csv(DATA_LOCATION + "data/identity/admin_users_tokens.dat").circular
  val mossoIDFeeder = csv(DATA_LOCATION + "data/identity/mossoid_staging.dat").circular
  val nastIDFeeder = csv(DATA_LOCATION + "data/identity/nastid_staging.dat").circular
  val userIDFeeder = csv(DATA_LOCATION + "data/identity/user_id.dat").circular
  val fedDomainFeeder = csv(DATA_LOCATION + "data/identity/dom_users_for_fed.dat").circular
  val defaultUsersFeeder = csv(DATA_LOCATION + "data/identity/default_users_tokens.dat").circular
  // Gives 500,000 numbers  between 1,000,000 and 2,000,000, which is well
  // within Java int range (which we want to force a create one user call)
  // for the domain.
  // Later we randomly pick from this to avoid collisions on the agents.
  // The probability of a collision with just 50,000 users is just
  // .1 % over 3 hours and close to 0 at 15 places precision at 500,000

  val int_list = List.range(1000000,2000000) 

  val integer_feeder_1 = for (e<- int_list) yield Map("next_int" -> e) 
  val integer_feeder = integer_feeder_1.toArray.random
  
  def v20_authenticate: ChainBuilder = {

    feed(usersFeeder)
      .exec(http("POST /v2.0/tokens")
      .post("/v2.0/tokens")
      .header("X-Forwarded-For", "${ipaddress}")
      .body(ElFileBody("request-bodies/identity/v2/tokens_body_v2.json")).asJSON
      .check(status.is(200)))
      .exitHereIfFailed
  }

  def v20_authenticate_default_user: ChainBuilder = {

    feed(defaultUsersFeeder)
      .exec(http("POST /v2.0/tokens")
        .post("/v2.0/tokens")
        .header("X-Forwarded-For", "${ipaddress}")
        .body(ElFileBody("request-bodies/identity/v2/tokens_body_v2.json")).asJSON
        .check(status.is(200)))
      .exitHereIfFailed
  }

  def v20_authenticate_rcn_roles: ChainBuilder = {

    feed(usersFeeder)
      .exec(http("POST /v2.0/tokens?apply_rcn_roles=true")
      .post("/v2.0/tokens?apply_rcn_roles=true")
      .header("X-Forwarded-For", "${ipaddress}")
      .body(ElFileBody("request-bodies/identity/v2/tokens_body_v2.json")).asJSON
      .check(status.is(200)))
      .exitHereIfFailed
  }
  def v20_saml_authenticate: ChainBuilder = {
    feed(fedDomainFeeder)
      .exec(http("POST /v2.0/RAX-AUTH/federation/saml/auth")
      .post("/v2.0/RAX-AUTH/federation/saml/auth")
      .header("X-Forwarded-For", "${ipaddress}")
      .header("Accept-Encoding", "identity")
      .header("Content-Type", "application/xml")
      .header("Identity-API-Version", "2.0")
      .body(StringBody(session => {
////         val output = command.!!
//         val baos = new ByteArrayOutputStream
//         val ps = new PrintStream(baos)
//         // val args = Array("/usr/bin/java", "-jar", "/root/identity-perf-agent/saml-generator.jar", "v2", "-domain="+ session("domainid").as[String], "-responseFlavor=v2DomainOrigin", "-originIssuer=https://perf-" + session("domainid").as[String] + ".issuer.com", "-roles=object-store:observer")
//         val args = Array("-domain", session("domainid").as[String], "-responseFlavor", "v2DomainOrigin", "-originIssuer", "https://perf-" + session("domainid").as[String] + ".issuer.com", "-roles", "object-store:observer")
//         ScalaConsole.withOut(ps)({
//		new FederatedAuthV2().processV2AuthRequest(args: _*)})
//         baos.toString()
         // output

     // Try #2
     val domain = session("domainid").as[String] 
     val username = String.format("User%s", UUID.randomUUID().toString())
     val email = "federated@rackspace.com"
     val roleNames = Set("object-store:observer")
     val brokerIssuer = null
     val originIssuer = "https://perf-" + domain+ ".issuer.com"
     val tokenExpirationSeconds = 5000
     val PASSWORD_PROTECTED_AUTHCONTEXT_REF_CLASS="urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport"
     val rawFlavor = "v2DomainOrigin"
     val responseFlavor = ResponseFlavor.fromFlavor(rawFlavor)
     val request= new FederatedDomainAuthGenerationRequest()
     request.setUsername(username)
     request.setEmail(email)
     request.setDomainId(domain)
     request.setRoleNames(roleNames)
     request.setBrokerIssuer(brokerIssuer)
     request.setOriginIssuer(originIssuer)
     request.setValiditySeconds(tokenExpirationSeconds)
     request.setAuthContextRefClass(PASSWORD_PROTECTED_AUTHCONTEXT_REF_CLASS)
     request.setRequestIssueInstant(new DateTime())
     request.setResponseFlavor(responseFlavor.asInstanceOf[ResponseFlavor])
     val response = generator.createSignedSAMLResponse(request)
     val result = generator.convertResponseToString(response)
     println(result)
      result
      })) 
      .check(status.is(200), status.saveAs("status"), bodyString.saveAs("responseBody")))
      .exitHereIfFailed
  }
  def v20_impersonate: ChainBuilder = {

    feed(usersFeeder_v20_admin)
      .feed(usersFeeder)
      .exec(http("POST /v2.0/tokens (impersonate)")
      .post("/v2.0/RAX-AUTH/impersonation-tokens")
      .header("X-Forwarded-For", "${ipaddress}")
      .header("x-auth-token", "${admin_token}")
      .body(ElFileBody("request-bodies/identity/v2/impersonate_body_v2.json")).asJSON
      .check(status.is(200)))
      .exitHereIfFailed
  }


def v11_authenticate: ChainBuilder = {

    feed(usersFeeder)
      .exec(http("POST /v1.1/auth")
      .post("/v1.1/auth")
      .header("X-Forwarded-For", "${ipaddress}")
      .body(ElFileBody("request-bodies/identity/v1/tokens_body_v1.json")).asJSON
      .check(status.is(200)))
      .exitHereIfFailed
  }

def v20_validate: ChainBuilder = {
    feed(usersFeeder_v20_admin)
      .feed(usersFeeder)
      .exec(http("GET /v2.0/tokens/{tokenid}")
      .get("/v2.0/tokens/${token}")
      .header("X-Forwarded-For", "${ipaddress}")
      .header("x-auth-token", "${admin_token}")
      .check(status.is(200)))
      .exitHereIfFailed
  }
def v20_validate_default_user: ChainBuilder = {
    feed(usersFeeder_v20_admin)
      .feed(defaultUsersFeeder)
      .exec(http("GET /v2.0/tokens/{tokenid}")
        .get("/v2.0/tokens/${token}")
        .header("X-Forwarded-For", "${ipaddress}")
        .header("x-auth-token", "${admin_token}")
        .check(status.is(200)))
      .exitHereIfFailed
  }

  def v20_validate_rcn_roles: ChainBuilder = {
    feed(usersFeeder_v20_admin)
      .feed(usersFeeder)
      .exec(http("GET /v2.0/tokens/{tokenid}?apply_rcn_roles=true")
      .get("/v2.0/tokens/${token}?apply_rcn_roles=true")
      .header("X-Forwarded-For", "${ipaddress}")
      .header("x-auth-token", "${admin_token}")
      .check(status.is(200)))
      .exitHereIfFailed
  }
def v11_get_user_by_mosso_id: ChainBuilder = {
      feed(usersFeeder_v20_admin)
      .feed(mossoIDFeeder)
      .exec(http("GET /v1.1/mosso/{mossoId}")
      .get("/v1.1/mosso/${mossoId}")
      .header("X-Forwarded-For", "${ipaddress}")
      .basicAuth("${user_name}", "${password}")
      .check(status.is(200)))
      .exitHereIfFailed
  }

def v11_get_user_in_domain_by_mosso_id: ChainBuilder = {
    feed(usersFeeder_v20_admin)
      .feed(mossoIDFeeder)
      .exec(http("GET /v2.0/RAX-AUTH/domains/{mossoId}/users?enabled=True")
      .get("/v2.0/RAX-AUTH/domains/${mossoId}/users?enabled=True")
      .header("X-Forwarded-For", "${ipaddress}")
      .header("x-auth-token", "${admin_token}")
      .check(status.is(200)))
      .exitHereIfFailed
  }
def v11_get_user_by_nast_id: ChainBuilder = {
      feed(usersFeeder_v20_admin)
      .feed(nastIDFeeder)
      .exec(http("GET /v1.1/nast/{nastid}")
      .get("/v1.1/nast/${nastid}")
      .header("X-Forwarded-For", "${ipaddress}")
      .basicAuth("${user_name}", "${password}")
      .check(status.is(200)))
      .exitHereIfFailed
  }
def v11_get_user_by_name: ChainBuilder = {
      feed(usersFeeder)
      .feed(usersFeeder_v20_admin)
      .exec(http("GET /v1.1/users/{username}")
      .get("/v1.1/users/${username}")
      .header("X-Forwarded-For", "${ipaddress}")
      .basicAuth("${user_name}", "${password}")
      .check(status.is(200)))
      .exitHereIfFailed
  }
def v20_list_groups_for_user_id: ChainBuilder = {
    feed(usersFeeder_v20_admin)
      .feed(userIDFeeder)
      .exec(http("GET /v2.0/users/{userId}/RAX-KSGRP")
      .get("/v2.0/users/${userId}/RAX-KSGRP")
      .header("X-Forwarded-For", "${ipaddress}")
      .header("x-auth-token", "${admin_token}")
      .check(status.is(200)))
      .exitHereIfFailed
  }
def v20_get_user_credentials_for_user_id: ChainBuilder = {
    feed(usersFeeder_v20_admin)
      .feed(userIDFeeder)
      .exec(http("GET /v2.0/users/{userId}/OS-KSADM/credentials/RAX-KSKEY:apiKeyCredentials")
      .get("/v2.0/users/${userId}/OS-KSADM/credentials/RAX-KSKEY:apiKeyCredentials")
      .header("X-Forwarded-For", "${ipaddress}")
      .header("x-auth-token", "${admin_token}")
      .check(status.is(200)))
      .exitHereIfFailed
  }
def v20_list_global_roles_for_user_id: ChainBuilder = {
    feed(usersFeeder_v20_admin)
      .feed(userIDFeeder)
      .exec(http("GET /v2.0/users/{userId}/roles")
      .get("/v2.0/users/${userId}/roles")
      .header("x-auth-token", "${admin_token}")
      .header("X-Forwarded-For", "${ipaddress}")
      .check(status.is(200)))
      .exitHereIfFailed
  }
  def v20_create_user: ChainBuilder = {
    feed(usersFeeder_v20_admin)
      .feed(integer_feeder)
      .exec(http("POST /v2.0/users")
      .post("/v2.0/users")
      .header("X-Forwarded-For", "${ipaddress}")
      .body(ElFileBody("request-bodies/identity/v2/create_user_body_v2.json")).asJSON
      .header("x-auth-token", "${admin_token}")
      .check(jsonPath("$.user.id").saveAs("user_id")))
      .exitHereIfFailed
  }
}

