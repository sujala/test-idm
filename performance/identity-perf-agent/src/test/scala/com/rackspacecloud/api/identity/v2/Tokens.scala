package com.rackspacecloud.api.identity.v2


import com.rackspacecloud.Options
import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import com.typesafe.config.ConfigFactory
import io.gatling.http.Predef._
import com.rackspacecloud.scenarios.Identity._

import scala.collection.mutable.ArrayBuffer
import scala.{Console => ScalaConsole}
import sys.process._
import org.opensaml.core.config.InitializationService
import collection.JavaConversions._
import com.rackspace.idm.federation.samlgenerator.v2.FederatedDomainAuthRequestGenerator
import com.rackspace.idm.federation.samlgenerator.v2.ResponseFlavor
import com.rackspace.idm.federation.samlgenerator.v2.FederatedDomainAuthGenerationRequest
import org.joda.time.DateTime
import com.rackspace.idm.federation.utils.SamlCredentialUtils
import java.util.UUID
import scala.util.parsing.json._
import java.util.concurrent.ThreadLocalRandom

object IdentityFeederCircular extends Feeder[String] {

  private var records: Iterator[Map[String, String]] = Iterator()

  private var cachedRecords: ArrayBuffer[Map[String, String]] = ArrayBuffer()

  override def next: Map[String, String] = records.next()

  override def hasNext: Boolean = {
    if (records.hasNext) {
      true
    } else {
      records = cachedRecords.toIterator
      true
    }
  }

  def appendRecord(record: Map[String, String]): Unit = {
    val arrayBuffer = ArrayBuffer[Map[String, String]]()
    records.foreach {
      x => arrayBuffer.append(x)
    }
    arrayBuffer.append(record)
    records = arrayBuffer.toIterator
    cachedRecords.append(record)
  }
}

object IdentityFeederAlternatingCircular extends Feeder[String] {

  private var records: Iterator[Map[String, String]] = Iterator()

  private var cachedRecords: ArrayBuffer[Map[String, String]] = ArrayBuffer()

  override def next: Map[String, String] = {

  records.next()

  if (!records.hasNext) {
    records = cachedRecords.toIterator
  }
    records.next()
  }

  override def hasNext: Boolean = {
    if (records.hasNext) {
      true
    } else {
      records = cachedRecords.toIterator
      true
    }
  }

  def appendRecord(record: Map[String, String]): Unit = {
    val arrayBuffer = ArrayBuffer[Map[String, String]]()
    records.foreach {
      x => arrayBuffer.append(x)
    }
    arrayBuffer.append(record)
    records = arrayBuffer.toIterator
    cachedRecords.append(record)
  }

}

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
  val daFeeder = IdentityFeederCircular
  val anotherDAFeeder = IdentityFeederAlternatingCircular
  val parentDADelegatePairingFeeder = IdentityFeederCircular
  val defaultUsersForChangePassword = csv(DATA_LOCATION + "data/identity/default_users.dat").circular

  // Can uncomment once we are ready to add this memory-leak test to regular perf suite
  // val usersInDomainFeeder = csv(DATA_LOCATION + "data/identity/users_in_domain.dat").circular

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
      .body(StringBody(session => {
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
      result
      }))
      .check(status.is(200), status.saveAs("status"), bodyString.saveAs("responseBody")))
      .exitHereIfFailed
  }
  def v20_saml_authenticate_same_user: ChainBuilder = {
    feed(fedDomainFeeder)
      .exec(http("POST /v2.0/RAX-AUTH/federation/saml/auth")
        .post("/v2.0/RAX-AUTH/federation/saml/auth")
        .header("X-Forwarded-For", "${ipaddress}")
        .header("Accept-Encoding", "identity")
        .header("Content-Type", "application/xml")
        .body(StringBody(session => {
          val domain = session("domainid").as[String]
          val username = s"fed_user_$domain"
          val email = "federated@rackspace.com"
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
          request.setBrokerIssuer(brokerIssuer)
          request.setOriginIssuer(originIssuer)
          request.setValiditySeconds(tokenExpirationSeconds)
          request.setAuthContextRefClass(PASSWORD_PROTECTED_AUTHCONTEXT_REF_CLASS)
          request.setRequestIssueInstant(new DateTime())
          request.setResponseFlavor(responseFlavor.asInstanceOf[ResponseFlavor])
          val response = generator.createSignedSAMLResponse(request)
          val result = generator.convertResponseToString(response)
          result
        }))
        .check(status.is(200), status.saveAs("status"), bodyString.saveAs("responseBody")))
      .exitHereIfFailed
  }
  def v20_saml_authenticate_same_user_old_endpoint: ChainBuilder = {
    feed(fedDomainFeeder)
      .exec(http("POST /v2.0/RAX-AUTH/saml-tokens")
        .post("/v2.0/RAX-AUTH/saml-tokens")
        .header("X-Forwarded-For", "${ipaddress}")
        .header("Accept-Encoding", "identity")
        .header("Content-Type", "application/xml")
        .body(StringBody(session => {
          val domain = session("domainid").as[String]
          val username = s"fed_user_$domain"
          val email = "federated@rackspace.com"
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
          request.setBrokerIssuer(brokerIssuer)
          request.setOriginIssuer(originIssuer)
          request.setValiditySeconds(tokenExpirationSeconds)
          request.setAuthContextRefClass(PASSWORD_PROTECTED_AUTHCONTEXT_REF_CLASS)
          request.setRequestIssueInstant(new DateTime())
          request.setResponseFlavor(responseFlavor.asInstanceOf[ResponseFlavor])
          val response = generator.createSignedSAMLResponse(request)
          val result = generator.convertResponseToString(response)
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

  /**
    * This function is added to test change password API
    *
    */

  def v20_change_password = {
    feed(defaultUsersForChangePassword)
      .exec(http("POST /v2.0/users/RAX-AUTH/change-pwd")
        .post("/v2.0/users/RAX-AUTH/change-pwd")
        .body(ElFileBody("request-bodies/identity/v2/change_password_v2.json")).asJSON
        .check(status.is(400)))
      .exitHereIfFailed

  }

  /**
    * This flow will test the cloud feed events
    * It will:
    *
    * * Create a user with default password (issue event)
    * * Retrieve user's token (issue event)
    * * Explicitly revoke user's token (issue event)
    * * Update user's password (issue a slew of events)
    * * Disable user (issue 2 events)
    * * Enable user (issue 2 events)
    * * Delete user (issue an event)
    *
    * @return
    */
  def v20_crud_users: ChainBuilder = {
    feed(usersFeeder_v20_admin)
      .feed(integer_feeder)
      .exec { session =>
        session.set("username_prefix", "perf_cf")}
      .exec(
        http("POST /v2.0/users")
          .post("/v2.0/users")
          .header("X-Forwarded-For", "${ipaddress}")
          .body(ElFileBody("request-bodies/identity/v2/create_user_body_v2.json")).asJSON
          .header("x-auth-token", "${admin_token}")
          .check(jsonPath("$.user.id").saveAs("user_id"))
          .check(status.is(201)))
      .exec { session =>
        created_users_writer.write {
          session("admin_token").as[String] + "," +
            session("user_id").as[String] +
            "\n"
        }; session}
      .exitHereIfFailed
      .exec(
        http("GET TOKEN FOR [user_id]")
          .post("/v2.0/tokens")
          .header("X-Forwarded-For", "${ipaddress}")
          .body(ElFileBody("request-bodies/identity/v2/tokens_body_password_v2.json")).asJSON
          .check(jsonPath("$.access.token.id").saveAs("user_token"))
          .check(status.is(200))
      )
      .exec(
        http("REVOKE TOKEN FOR [user_id]")
          .delete("/v2.0/tokens/${user_token}")
          .header("X-Forwarded-For", "${ipaddress}")
          .header("x-auth-token", "${admin_token}")
      )
      .exec(
        http("UPDATE /v2.0/users/[user_id]")
          .post("/v2.0/users/${user_id}")
          .header("X-Forwarded-For", "${ipaddress}")
          .header("x-auth-token", "${admin_token}")
          .body(ElFileBody("request-bodies/identity/v2/update_user_password_v2.json")).asJSON
      )
      .exitHereIfFailed
      .exec(
        http("DISABLE /v2.0/users/[user_id]")
          .post("/v2.0/users/${user_id}")
          .header("X-Forwarded-For", "${ipaddress}")
          .header("x-auth-token", "${admin_token}")
          .body(ElFileBody("request-bodies/identity/v2/disable_user_body_v2.json")).asJSON
      )
      .exitHereIfFailed
      .exec(
        http("ENABLE /v2.0/users/[user_id]")
          .post("/v2.0/users/${user_id}")
          .header("X-Forwarded-For", "${ipaddress}")
          .header("x-auth-token", "${admin_token}")
          .body(ElFileBody("request-bodies/identity/v2/enable_user_body_v2.json")).asJSON
      )
      .exitHereIfFailed
      .exec(
        http("DELETE /v2.0/users/[user_id]")
          .delete("/v2.0/users/${user_id}")
          .header("X-Forwarded-For", "${ipaddress}")
          .header("x-auth-token", "${admin_token}")
      )
      .exitHereIfFailed
      .exec(
        http("DISABLE /v2.0/RAX-AUTH/domains")
          .put("/v2.0/RAX-AUTH/domains/${next_int}")
          .body(ElFileBody("request-bodies/identity/v2/disable_domain_body.json")).asJSON
          .header("X-Forwarded-For", "${ipaddress}")
          .header("x-auth-token", "${admin_token}")
      )
      .exitHereIfFailed
      .exec(
        http("DELETE /v2.0/RAX-AUTH/domains")
          .delete("/v2.0/RAX-AUTH/domains/${next_int}")
          .header("X-Forwarded-For", "${ipaddress}")
          .header("x-auth-token", "${admin_token}")
      )
      .exitHereIfFailed

  }


  /**
    * This flow will test the list roles for identity:admin
    *
    * @return
    */
  def v20_list_roles_for_identity_admin: ChainBuilder = {
    feed(usersFeeder_v20_admin)
      .exec(
        http("GET TOKEN FOR admin_user")
          .post("/v2.0/tokens")
          .header("X-Forwarded-For", "${ipaddress}")
          .header("Content-type", "application/json")
          .body(StringBody("""{"auth": {"passwordCredentials": {"username": "${user_name}","password": "${password}"}}}""")).asJSON
          .check(status.is(200))
          .check(jsonPath("$.access.token.id").saveAs("user_token"))
      )
      .exitHereIfFailed
      .exec(
        http("LIST ROLES")
          .get("/v2.0/OS-KSADM/roles?limit=1000")
          .header("X-Forwarded-For", "${ipaddress}")
          .header("x-auth-token", "${user_token}")
          .check(status.is(200))
      )
      .exitHereIfFailed

  }

  def v20_create_delegation_agreement: ChainBuilder = {

    feed(usersFeeder)
      .feed(integer_feeder)
      .exec(
        http("GET TOKEN FOR user:admin_user")
          .post("/v2.0/tokens")
          .header("X-Forwarded-For", "${ipaddress}")
          .header("Content-type", "application/json")
          .body(StringBody("""{"auth": {"RAX-KSKEY:apiKeyCredentials": {"username": "${username}","apiKey": "${apikey}"}}}""")).asJSON
          .check(status.is(200))
          .check(jsonPath("$.access.token.id").saveAs("user_token"))
      )
      .exitHereIfFailed
      .exec(
        http("CREATE DELEGATION AGREEMENT")
          .post("/v2.0/RAX-AUTH/delegation-agreements")
          .header("X-Forwarded-For", "${ipaddress}")
          .header("x-auth-token", "${user_token}")
          .body(ElFileBody("request-bodies/identity/v2/create_delegation_agreement_body.json")).asJSON
          .check(status.is(201))
          .check(jsonPath("$['RAX-AUTH:delegationAgreement'].id").saveAs("parent_da_id"))
          .check(jsonPath("$['RAX-AUTH:delegationAgreement'].principalId").saveAs("principal_id"))
      )
      .exec { session =>
        daFeeder.appendRecord(
          Map(
            "parent_da_id" -> session("parent_da_id").as[String],
            "principal_token" -> session("user_token").as[String]
          ))
        anotherDAFeeder.appendRecord(Map(
          "user_id" -> session("principal_id").as[String],
          "delegate_token" -> session("user_token").as[String])
        )
        session}
      .exitHereIfFailed

  }

  def v20_add_user_delegate: ChainBuilder = {

    feed(usersFeeder)
      .feed(daFeeder)
      .feed(anotherDAFeeder)
      .exec (
        http("ADD USER DELEGATE TO DELEGATION AGREEMENT")
          .put("/v2.0/RAX-AUTH/delegation-agreements/${parent_da_id}/delegates/users/${user_id}")
          .header("X-Forwarded-For", "${ipaddress}")
          .header("x-auth-token", "${principal_token}")
          .check(status.is(204)))
      .exec { session =>
        parentDADelegatePairingFeeder.appendRecord(
          Map(
            "parent_da_id" -> session("parent_da_id").as[String],
            "delegate_token" -> session("delegate_token").as[String]
          ))
        session}
      .exitHereIfFailed
  }

  def v20_create_nested_delegation_agreement: ChainBuilder = {

    feed(usersFeeder)
      .feed(parentDADelegatePairingFeeder)
      .exec(
        http("CREATE NESTED DELEGATION AGREEMENT")
          .post("/v2.0/RAX-AUTH/delegation-agreements")
          .header("X-Forwarded-For", "${ipaddress}")
          .header("x-auth-token", "${delegate_token}")
          .body(StringBody("""{"RAX-AUTH:delegationAgreement": {"name": "perf_test_nest_da", "parentDelegationAgreementId": "${parent_da_id}"}}""")).asJSON
          .check(status.is(201))
      )
      .exitHereIfFailed

  }

  def v20_delete_delegation_agreement: ChainBuilder = {

    feed(usersFeeder)
      .feed(daFeeder)
      .exec(
        http("DELETE DELEGATION AGREEMENT")
          .delete("/v2.0/RAX-AUTH/delegation-agreements/${parent_da_id}")
          .header("X-Forwarded-For", "${ipaddress}")
          .header("x-auth-token", "${principal_token}")
          .check(status.is(204))
      )
      .exitHereIfFailed
  }

}