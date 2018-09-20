package com.rackspacecloud.simulations.identity

import com.rackspacecloud.scenarios._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.commons.validation._
import java.io._

import io.gatling.core.structure._
import io.gatling.core.protocol.Protocol

import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory

class IdentityConstantTputUsersRampUp extends Simulation {
  val conf = ConfigFactory.load()


  // V20 Authenticate
  val V20_AUTHENTICATE_APIKEY_USERS_PER_SEC : Double =  conf.getDouble("soa.v20_authenticate_apikey.users_per_sec")
  val V20_AUTHENTICATE_APIKEY_REPL_USERS_PER_SEC : Double =  conf.getDouble("soa.v20_authenticate_apikey_repl.users_per_sec")
  val V20_AUTHENTICATE_APIKEY_INTERNAL_USERS_PER_SEC : Double =  conf.getDouble("soa.v20_authenticate_apikey_internal.users_per_sec")
  val V20_AUTHENTICATE_APIKEY_INTERNAL_REPL_USERS_PER_SEC : Double =  conf.getDouble("soa.v20_authenticate_apikey_internal_repl.users_per_sec")
  val V20_AUTHENTICATE_APIKEY_DEFAULT_USERS_PER_SEC : Double =  conf.getDouble("soa.v20_authenticate_apikey_default_users.users_per_sec")


  val v20_apikey_auth_scn = Identity.v20_apikey_auth
  val v20_apikey_auth_rcn_roles = Identity.v20_apikey_auth_rcn_roles
  val v20_apikey_auth_scn_repl = Identity.v20_apikey_auth_repl
  val v20_apikey_auth_scn_internal = Identity.v20_apikey_auth_internal
  val v20_apikey_auth_scn_internal_repl = Identity.v20_apikey_auth_internal_repl

  // V20 SAML Authenticate
  val V20_AUTHENTICATE_SAML_USERS_PER_SEC : Double =  conf.getDouble("soa.v20_authenticate_saml.users_per_sec")
  val V20_AUTHENTICATE_SAML_REPL_USERS_PER_SEC : Double =  conf.getDouble("soa.v20_authenticate_saml_repl.users_per_sec")
  val V20_AUTHENTICATE_SAML_INTERNAL_USERS_PER_SEC : Double =  conf.getDouble("soa.v20_authenticate_saml_internal.users_per_sec")
  val V20_AUTHENTICATE_SAML_INTERNAL_REPL_USERS_PER_SEC : Double =  conf.getDouble("soa.v20_authenticate_saml_internal_repl.users_per_sec")
  val V20_SAME_USER_SAML_AUTH_USERS_PER_SEC : Double =  conf.getDouble("soa.v20_same_user_authenticate_saml.users_per_sec")

  val v20_saml_auth_scn = Identity.v20_saml_auth
  val v20_saml_auth_scn_repl = Identity.v20_saml_auth_repl
  val v20_saml_auth_scn_internal = Identity.v20_saml_auth_internal
  val v20_saml_auth_scn_internal_repl = Identity.v20_saml_auth_internal_repl
  val v20_saml_auth_same_user_scn = Identity.v20_saml_auth_same_user
  val v20_saml_auth_same_user_old_endpoint_scn = Identity.v20_saml_auth_same_user_old_endpoint

  // V20 Impersonate
  val V20_IMPERSONATE_USERS_PER_SEC : Double =  conf.getDouble("soa.v20_impersonate.users_per_sec")
  val V20_IMPERSONATE_REPL_USERS_PER_SEC : Double =  conf.getDouble("soa.v20_impersonate_repl.users_per_sec")
  val V20_IMPERSONATE_INTERNAL_USERS_PER_SEC : Double =  conf.getDouble("soa.v20_impersonate_internal.users_per_sec")
  val V20_IMPERSONATE_INTERNAL_REPL_USERS_PER_SEC : Double =  conf.getDouble("soa.v20_impersonate_internal_repl.users_per_sec")

  val v20_impersonate_scn = Identity.v20_impersonate
  val v20_impersonate_scn_repl = Identity.v20_impersonate_repl
  val v20_impersonate_scn_internal = Identity.v20_impersonate_internal
  val v20_impersonate_scn_internal_repl = Identity.v20_impersonate_internal_repl



  // V11 Authenticate
  val V11_AUTHENTICATE_APIKEY_USERS_PER_SEC : Double =  conf.getDouble("soa.v11_authenticate_apikey.users_per_sec")
  val V11_AUTHENTICATE_APIKEY_REPL_USERS_PER_SEC : Double =  conf.getDouble("soa.v11_authenticate_apikey_repl.users_per_sec")

  val v11_apikey_auth_scn = Identity.v11_apikey_auth
  val v11_apikey_auth_repl_scn = Identity.v11_apikey_auth_repl


  // V20 Validate
  val V20_VALIDATE_USERS_PER_SEC : Double =  conf.getDouble("soa.v20_validate.users_per_sec")
  val V20_VALIDATE_REPL_USERS_PER_SEC : Double =  conf.getDouble("soa.v20_validate_repl.users_per_sec")
  val V20_VALIDATE_INTERNAL_USERS_PER_SEC : Double =  conf.getDouble("soa.v20_validate_internal.users_per_sec")
  val V20_VALIDATE_INTERNAL_REPL_USERS_PER_SEC : Double =  conf.getDouble("soa.v20_validate_internal_repl.users_per_sec")
  val V20_VALIDATE_DEFAULT_USERS_PER_SEC : Double =  conf.getDouble("soa.v20_validate_default_users.users_per_sec")

  val v20_token_validate_scn = Identity.v20_token_validate
  val v20_token_validate_scn_repl = Identity.v20_token_validate_repl
  val v20_token_validate_scn_internal = Identity.v20_token_validate_internal
  val v20_token_validate_scn_internal_repl = Identity.v20_token_validate_internal_repl


  // V11 Get User By Mosso Id
  val V11_GET_USER_BY_MOSSO_ID_USERS_PER_SEC : Double = conf.getDouble("soa.v11_get_user_by_mosso_id.users_per_sec")
  val V11_GET_USER_BY_MOSSO_ID_REPL_USERS_PER_SEC : Double = conf.getDouble("soa.v11_get_user_by_mosso_id_repl.users_per_sec")
  val V11_GET_USER_BY_MOSSO_ID_INTERNAL_USERS_PER_SEC : Double = conf.getDouble("soa.v11_get_user_by_mosso_id_internal.users_per_sec")
  val V11_GET_USER_BY_MOSSO_ID_INTERNAL_REPL_USERS_PER_SEC : Double = conf.getDouble("soa.v11_get_user_by_mosso_id_internal_repl.users_per_sec")

  val v11_get_user_by_mosso_id_scn = Identity.v11_get_user_by_mosso_id
  val v11_get_user_by_mosso_id_repl_scn = Identity.v11_get_user_by_mosso_id_repl
  val v11_get_user_by_mosso_id_internal_scn = Identity.v11_get_user_by_mosso_id_internal
  val v11_get_user_by_mosso_id_internal_repl_scn = Identity.v11_get_user_by_mosso_id_internal_repl

  // V11 Get User In Domain By Mosso Id
  val V11_GET_USER_IN_DOMAIN_BY_MOSSO_ID_USERS_PER_SEC : Double = conf.getDouble("soa.v11_get_user_in_domain_by_mosso_id.users_per_sec")
  val V11_GET_USER_IN_DOMAIN_BY_MOSSO_ID_REPL_USERS_PER_SEC : Double = conf.getDouble("soa.v11_get_user_in_domain_by_mosso_id_repl.users_per_sec")
  val V11_GET_USER_IN_DOMAIN_BY_MOSSO_ID_INTERNAL_USERS_PER_SEC : Double = conf.getDouble("soa.v11_get_user_in_domain_by_mosso_id_internal.users_per_sec")
  val V11_GET_USER_IN_DOMAIN_BY_MOSSO_ID_INTERNAL_REPL_USERS_PER_SEC : Double = conf.getDouble("soa.v11_get_user_in_domain_by_mosso_id_internal_repl.users_per_sec")

  val v11_get_user_in_domain_by_mosso_id_scn = Identity.v11_get_user_in_domain_by_mosso_id
  val v11_get_user_in_domain_by_mosso_id_repl_scn = Identity.v11_get_user_in_domain_by_mosso_id_repl
  val v11_get_user_in_domain_by_mosso_id_internal_scn = Identity.v11_get_user_in_domain_by_mosso_id_internal
  val v11_get_user_in_domain_by_mosso_id_internal_repl_scn = Identity.v11_get_user_in_domain_by_mosso_id_internal_repl

  // V11 Get User By Nast Id
  val V11_GET_USER_BY_NAST_ID_USERS_PER_SEC : Double = conf.getDouble("soa.v11_get_user_by_nast_id.users_per_sec")
  val V11_GET_USER_BY_NAST_ID_REPL_USERS_PER_SEC : Double = conf.getDouble("soa.v11_get_user_by_nast_id_repl.users_per_sec")
  val V11_GET_USER_BY_NAST_ID_INTERNAL_USERS_PER_SEC : Double = conf.getDouble("soa.v11_get_user_by_nast_id_internal.users_per_sec")
  val V11_GET_USER_BY_NAST_ID_INTERNAL_REPL_USERS_PER_SEC : Double = conf.getDouble("soa.v11_get_user_by_nast_id_internal_repl.users_per_sec")

  val v11_get_user_by_nast_id_scn = Identity.v11_get_user_by_nast_id
  val v11_get_user_by_nast_id_repl_scn = Identity.v11_get_user_by_nast_id_repl
  val v11_get_user_by_nast_id_internal_scn = Identity.v11_get_user_by_nast_id_internal
  val v11_get_user_by_nast_id_internal_repl_scn = Identity.v11_get_user_by_nast_id_internal_repl

  // V11 Get User By Name
  val V11_GET_USER_BY_NAME_USERS_PER_SEC : Double =  conf.getDouble("soa.v11_get_user_by_name.users_per_sec")
  val V11_GET_USER_BY_NAME_REPL_USERS_PER_SEC : Double =  conf.getDouble("soa.v11_get_user_by_name_repl.users_per_sec")
  val V11_GET_USER_BY_NAME_INTERNAL_USERS_PER_SEC : Double =  conf.getDouble("soa.v11_get_user_by_name_internal.users_per_sec")
  val V11_GET_USER_BY_NAME_INTERNAL_REPL_USERS_PER_SEC : Double =  conf.getDouble("soa.v11_get_user_by_name_internal_repl.users_per_sec")

  val v11_get_user_by_name_scn = Identity.v11_get_user_by_name
  val v11_get_user_by_name_scn_repl = Identity.v11_get_user_by_name_repl
  val v11_get_user_by_name_scn_internal = Identity.v11_get_user_by_name_internal
  val v11_get_user_by_name_scn_internal_repl = Identity.v11_get_user_by_name_internal_repl


 // V20 List Groups for User Id
  val V20_LIST_GROUPS_FOR_USER_ID_USERS_PER_SEC : Double = conf.getDouble("soa.v20_list_groups_for_user_id.users_per_sec")
  val V20_LIST_GROUPS_FOR_USER_ID_REPL_USERS_PER_SEC : Double = conf.getDouble("soa.v20_list_groups_for_user_id_repl.users_per_sec")
  val V20_LIST_GROUPS_FOR_USER_ID_INTERNAL_USERS_PER_SEC : Double = conf.getDouble("soa.v20_list_groups_for_user_id_internal.users_per_sec")
  val V20_LIST_GROUPS_FOR_USER_ID_INTERNAL_REPL_USERS_PER_SEC : Double = conf.getDouble("soa.v20_list_groups_for_user_id_internal_repl.users_per_sec")

  val v20_list_groups_for_user_id_scn = Identity.v20_list_groups_for_user_id
  val v20_list_groups_for_user_id_repl_scn = Identity.v20_list_groups_for_user_id_repl
  val v20_list_groups_for_user_id_internal_scn = Identity.v20_list_groups_for_user_id_internal
  val v20_list_groups_for_user_id_internal_repl_scn = Identity.v20_list_groups_for_user_id_internal_repl

  val V20_LIST_ROLES_FOR_IDENTITY_ADMIN_USERS_PER_SEC : Double = conf.getDouble("soa.v20_list_all_roles.users_per_sec")
  val v20_list_all_roles_scn = Identity.v20_list_all_roles


 // V20 Get User Credentials for User Id
  val V20_GET_USER_CREDENTIALS_FOR_USER_ID_USERS_PER_SEC : Double = conf.getDouble("soa.v20_get_user_credentials_for_user_id.users_per_sec")
  val V20_GET_USER_CREDENTIALS_FOR_USER_ID_REPL_USERS_PER_SEC : Double = conf.getDouble("soa.v20_get_user_credentials_for_user_id_repl.users_per_sec")
  val V20_GET_USER_CREDENTIALS_FOR_USER_ID_INTERNAL_USERS_PER_SEC : Double = conf.getDouble("soa.v20_get_user_credentials_for_user_id_internal.users_per_sec")
  val V20_GET_USER_CREDENTIALS_FOR_USER_ID_INTERNAL_REPL_USERS_PER_SEC : Double = conf.getDouble("soa.v20_get_user_credentials_for_user_id_internal_repl.users_per_sec")

  val v20_get_user_credentials_for_user_id_scn = Identity.v20_get_user_credentials_for_user_id
  val v20_get_user_credentials_for_user_id_repl_scn = Identity.v20_get_user_credentials_for_user_id_repl
  val v20_get_user_credentials_for_user_id_internal_scn = Identity.v20_get_user_credentials_for_user_id_internal
  val v20_get_user_credentials_for_user_id_internal_repl_scn = Identity.v20_get_user_credentials_for_user_id_internal_repl

// V20 List Global Roles for User Id
  val V20_LIST_GLOBAL_ROLES_FOR_USER_ID_USERS_PER_SEC : Double = conf.getDouble("soa.v20_list_global_roles_for_user_id.users_per_sec")
  val V20_LIST_GLOBAL_ROLES_FOR_USER_ID_REPL_USERS_PER_SEC : Double = conf.getDouble("soa.v20_list_global_roles_for_user_id_repl.users_per_sec")
  val V20_LIST_GLOBAL_ROLES_FOR_USER_ID_INTERNAL_USERS_PER_SEC : Double = conf.getDouble("soa.v20_list_global_roles_for_user_id_internal.users_per_sec")
  val V20_LIST_GLOBAL_ROLES_FOR_USER_ID_INTERNAL_REPL_USERS_PER_SEC : Double = conf.getDouble("soa.v20_list_global_roles_for_user_id_internal_repl.users_per_sec")

  val v20_list_global_roles_for_user_id_scn = Identity.v20_list_global_roles_for_user_id
  val v20_list_global_roles_for_user_id_repl_scn = Identity.v20_list_global_roles_for_user_id_repl
  val v20_list_global_roles_for_user_id_internal_scn = Identity.v20_list_global_roles_for_user_id_internal
  val v20_list_global_roles_for_user_id_internal_repl_scn = Identity.v20_list_global_roles_for_user_id_internal_repl

// V20 Create User 
  val V20_CREATE_USER_USERS_PER_SEC : Double = conf.getDouble("soa.v20_create_user.users_per_sec")
  val V20_CREATE_USER_REPL_USERS_PER_SEC : Double = conf.getDouble("soa.v20_create_user_repl.users_per_sec")
  val V20_CREATE_USER_INTERNAL_USERS_PER_SEC : Double = conf.getDouble("soa.v20_create_user_internal.users_per_sec")
  val V20_CREATE_USER_INTERNAL_REPL_USERS_PER_SEC : Double = conf.getDouble("soa.v20_create_user_internal_repl.users_per_sec")

  // V20 crud user

  val V20_CRUD_USER_USERS_PER_SEC : Double = conf.getDouble("soa.v20_crud_user.users_per_sec")
  val DAS_PER_SEC : Double =  conf.getDouble("soa.parent_das_per_sec")
  val MAX_DURATION_FOR_DAS_SECS: Int = conf.getInt("simulation.max_duration_secs_for_das")

  val v20_create_user_scn = Identity.v20_create_user
//   val v20_create_user_repl_scn = Identity.v20_create_user_repl
//   val v20_create_user_internal_scn = Identity.v20_create_user_internal
//   val v20_create_user_internal_repl_scn = Identity.v20_create_user_internal_repl

  val v20_apikey_auth_default_user_scn = Identity.v20_apikey_auth_default_user
  val v20_validate_default_user_token_scn = Identity.v20_token_validate_default_user

  val v20_crud_user_scn = Identity.v20_crud_user

  val create_da_scn = DelegationAgreementCreation.create_parent_das
  val add_user_delegate_scn = AddUserDelegate.add_user_delegate
  val create_nested_da_scn = CreateNestedDAs.create_nested_das
  val delete_parent_da_scn = DeleteParentDAs.delete_parent_das



  // Not method or version specific.
  val MAX_DURATION_SECS: Int = conf.getInt("simulation.max_duration_secs")
  val MAX_DURATION_FOR_SAML_SECS: Int = conf.getInt("simulation.max_duration_for_saml_secs")
  val OS_MAIN_EXTERNAL_AUTH_URL = conf.getString("main_external_auth_url")
  val OS_MAIN_INTERNAL_AUTH_URL = conf.getString("main_internal_auth_url")
  val OS_REPL_EXTERNAL_AUTH_URL = conf.getString("repl_external_auth_url")
  val OS_REPL_INTERNAL_AUTH_URL = conf.getString("repl_internal_auth_url")


  val httpMainExternalConf = http
    .baseURL(OS_MAIN_EXTERNAL_AUTH_URL)
    .acceptHeader("application/json")
    .acceptEncodingHeader("gzip, deflate, compress")
    .contentTypeHeader("application/json; charset=utf-8")
    .userAgentHeader( """QEPerf/1.0.0""")
    .shareConnections 
  
   val httpMainInternalConf = http
    .baseURL(OS_MAIN_INTERNAL_AUTH_URL)
    .acceptHeader("application/json")
    .acceptEncodingHeader("gzip, deflate, compress")
    .contentTypeHeader("application/json; charset=utf-8")
    .userAgentHeader( """QEPerf/1.0.0""")
    .shareConnections

  val httpReplExternalConf = http
    .baseURL(OS_REPL_EXTERNAL_AUTH_URL)
    .acceptHeader("application/json")
    .acceptEncodingHeader("gzip, deflate, compress")
    .contentTypeHeader("application/json; charset=utf-8")
    .userAgentHeader( """QEPerf/1.0.0""")
    .shareConnections

  val httpReplInternalConf = http
    .baseURL(OS_REPL_INTERNAL_AUTH_URL)
    .acceptHeader("application/json")
    .acceptEncodingHeader("gzip, deflate, compress")
    .contentTypeHeader("application/json; charset=utf-8")
    .userAgentHeader( """QEPerf/1.0.0""")
    .shareConnections



  def scn_wrapper(scenario: ScenarioBuilder, users_per_sec: Double, duration: Int, protocol_conf: Protocol, wait_time: Int=0): PopulationBuilder= {
    var local_users_per_sec = users_per_sec;
    var local_duration = duration;    
    if (users_per_sec <= 0.05){
         local_users_per_sec = 1;
         local_duration = 1;
    }
    scenario.inject(constantUsersPerSec(local_users_per_sec) during(local_duration seconds)).protocols(protocol_conf)
  }
  def list_scns(): List[PopulationBuilder] = {
     return List(
     scn_wrapper(v11_apikey_auth_scn, V11_AUTHENTICATE_APIKEY_USERS_PER_SEC, MAX_DURATION_SECS, httpMainExternalConf),
     scn_wrapper(v11_apikey_auth_repl_scn, V11_AUTHENTICATE_APIKEY_REPL_USERS_PER_SEC, MAX_DURATION_SECS, httpReplExternalConf),
    // V20 Authenticate
    scn_wrapper(v20_apikey_auth_scn, V20_AUTHENTICATE_APIKEY_USERS_PER_SEC, MAX_DURATION_SECS, httpMainExternalConf),
    scn_wrapper(v20_apikey_auth_rcn_roles, V20_AUTHENTICATE_APIKEY_USERS_PER_SEC, MAX_DURATION_SECS, httpMainExternalConf),
    scn_wrapper(v20_apikey_auth_scn_repl, V20_AUTHENTICATE_APIKEY_REPL_USERS_PER_SEC, MAX_DURATION_SECS, httpReplExternalConf),
    scn_wrapper(v20_apikey_auth_scn_internal, V20_AUTHENTICATE_APIKEY_INTERNAL_USERS_PER_SEC, MAX_DURATION_SECS, httpMainInternalConf),
    scn_wrapper(v20_apikey_auth_scn_internal_repl, V20_AUTHENTICATE_APIKEY_INTERNAL_REPL_USERS_PER_SEC, MAX_DURATION_SECS, httpReplInternalConf),
    scn_wrapper(v20_apikey_auth_default_user_scn, V20_AUTHENTICATE_APIKEY_DEFAULT_USERS_PER_SEC, MAX_DURATION_SECS, httpMainExternalConf),
    // V20 SAML Authenticate
    scn_wrapper(v20_saml_auth_scn, V20_AUTHENTICATE_SAML_USERS_PER_SEC, MAX_DURATION_FOR_SAML_SECS, httpMainExternalConf),
    scn_wrapper(v20_saml_auth_scn_repl, V20_AUTHENTICATE_SAML_REPL_USERS_PER_SEC, MAX_DURATION_SECS, httpReplExternalConf),
    scn_wrapper(v20_saml_auth_scn_internal, V20_AUTHENTICATE_SAML_INTERNAL_USERS_PER_SEC, MAX_DURATION_FOR_SAML_SECS, httpMainInternalConf),
    scn_wrapper(v20_saml_auth_scn_internal_repl, V20_AUTHENTICATE_SAML_INTERNAL_REPL_USERS_PER_SEC, MAX_DURATION_SECS, httpReplInternalConf),
    scn_wrapper(v20_saml_auth_same_user_scn, V20_SAME_USER_SAML_AUTH_USERS_PER_SEC, MAX_DURATION_FOR_SAML_SECS, httpMainExternalConf),
    scn_wrapper(v20_saml_auth_same_user_old_endpoint_scn, V20_SAME_USER_SAML_AUTH_USERS_PER_SEC, MAX_DURATION_FOR_SAML_SECS, httpMainExternalConf),

    // V20 Impersonate
    scn_wrapper(v20_impersonate_scn, V20_IMPERSONATE_USERS_PER_SEC, MAX_DURATION_SECS, httpMainExternalConf),
    scn_wrapper(v20_impersonate_scn_repl, V20_IMPERSONATE_REPL_USERS_PER_SEC, MAX_DURATION_SECS, httpReplExternalConf),
    scn_wrapper(v20_impersonate_scn_internal, V20_IMPERSONATE_INTERNAL_USERS_PER_SEC, MAX_DURATION_SECS, httpMainInternalConf),
    scn_wrapper(v20_impersonate_scn_internal_repl, V20_IMPERSONATE_INTERNAL_REPL_USERS_PER_SEC, MAX_DURATION_SECS, httpReplInternalConf),

     scn_wrapper(v20_token_validate_scn, V20_VALIDATE_USERS_PER_SEC, MAX_DURATION_SECS, httpMainExternalConf),
    scn_wrapper(v20_token_validate_scn_repl, V20_VALIDATE_REPL_USERS_PER_SEC, MAX_DURATION_SECS, httpReplExternalConf),
    scn_wrapper(v20_token_validate_scn_internal, V20_VALIDATE_INTERNAL_USERS_PER_SEC, MAX_DURATION_SECS, httpMainInternalConf),
    scn_wrapper(v20_token_validate_scn_internal_repl, V20_VALIDATE_INTERNAL_REPL_USERS_PER_SEC, MAX_DURATION_SECS, httpReplInternalConf),
    scn_wrapper(v20_validate_default_user_token_scn, V20_VALIDATE_DEFAULT_USERS_PER_SEC, MAX_DURATION_SECS, httpMainExternalConf),


    // Get User By Mosso Id
    scn_wrapper(v11_get_user_by_mosso_id_scn, V11_GET_USER_BY_MOSSO_ID_USERS_PER_SEC, MAX_DURATION_SECS, httpMainExternalConf),
    scn_wrapper(v11_get_user_by_mosso_id_repl_scn, V11_GET_USER_BY_MOSSO_ID_REPL_USERS_PER_SEC, MAX_DURATION_SECS, httpMainExternalConf),
    scn_wrapper(v11_get_user_by_mosso_id_internal_scn, V11_GET_USER_BY_MOSSO_ID_INTERNAL_USERS_PER_SEC, MAX_DURATION_SECS, httpReplExternalConf),
    scn_wrapper(v11_get_user_by_mosso_id_internal_repl_scn, V11_GET_USER_BY_MOSSO_ID_INTERNAL_REPL_USERS_PER_SEC, MAX_DURATION_SECS, httpReplInternalConf),

    // Get User In Domain By Mosso Id
    scn_wrapper(v11_get_user_in_domain_by_mosso_id_scn, V11_GET_USER_IN_DOMAIN_BY_MOSSO_ID_USERS_PER_SEC, MAX_DURATION_SECS, httpMainExternalConf),
    scn_wrapper(v11_get_user_in_domain_by_mosso_id_repl_scn, V11_GET_USER_IN_DOMAIN_BY_MOSSO_ID_REPL_USERS_PER_SEC, MAX_DURATION_SECS, httpMainExternalConf),
    scn_wrapper(v11_get_user_in_domain_by_mosso_id_internal_scn, V11_GET_USER_IN_DOMAIN_BY_MOSSO_ID_INTERNAL_USERS_PER_SEC, MAX_DURATION_SECS, httpReplExternalConf),
    scn_wrapper(v11_get_user_in_domain_by_mosso_id_internal_repl_scn, V11_GET_USER_IN_DOMAIN_BY_MOSSO_ID_INTERNAL_REPL_USERS_PER_SEC, MAX_DURATION_SECS, httpReplInternalConf),


    // Get User By Nast Id
    scn_wrapper(v11_get_user_by_nast_id_scn, V11_GET_USER_BY_NAST_ID_USERS_PER_SEC, MAX_DURATION_SECS, httpMainExternalConf),
    scn_wrapper(v11_get_user_by_nast_id_repl_scn, V11_GET_USER_BY_NAST_ID_REPL_USERS_PER_SEC, MAX_DURATION_SECS, httpMainExternalConf),
    scn_wrapper(v11_get_user_by_nast_id_internal_scn, V11_GET_USER_BY_NAST_ID_INTERNAL_USERS_PER_SEC, MAX_DURATION_SECS, httpReplExternalConf),
    scn_wrapper(v11_get_user_by_nast_id_internal_repl_scn, V11_GET_USER_BY_NAST_ID_INTERNAL_REPL_USERS_PER_SEC, MAX_DURATION_SECS, httpReplInternalConf),

    // Get User By Name
    scn_wrapper(v11_get_user_by_name_scn, V11_GET_USER_BY_NAME_USERS_PER_SEC, MAX_DURATION_SECS, httpMainExternalConf),
    scn_wrapper(v11_get_user_by_name_scn_repl, V11_GET_USER_BY_NAME_REPL_USERS_PER_SEC, MAX_DURATION_SECS, httpReplExternalConf),
    scn_wrapper(v11_get_user_by_name_scn_internal, V11_GET_USER_BY_NAME_INTERNAL_USERS_PER_SEC, MAX_DURATION_SECS, httpMainInternalConf),
    scn_wrapper(v11_get_user_by_name_scn_internal_repl, V11_GET_USER_BY_NAME_INTERNAL_REPL_USERS_PER_SEC, MAX_DURATION_SECS, httpReplInternalConf),


    // List Groups By User Id
    scn_wrapper(v20_list_groups_for_user_id_scn, V20_LIST_GROUPS_FOR_USER_ID_USERS_PER_SEC, MAX_DURATION_SECS, httpMainExternalConf),
    scn_wrapper(v20_list_groups_for_user_id_repl_scn, V20_LIST_GROUPS_FOR_USER_ID_REPL_USERS_PER_SEC, MAX_DURATION_SECS, httpMainExternalConf),
    scn_wrapper(v20_list_groups_for_user_id_internal_scn, V20_LIST_GROUPS_FOR_USER_ID_INTERNAL_USERS_PER_SEC, MAX_DURATION_SECS, httpReplExternalConf),
    scn_wrapper(v20_list_groups_for_user_id_internal_repl_scn, V20_LIST_GROUPS_FOR_USER_ID_INTERNAL_REPL_USERS_PER_SEC, MAX_DURATION_SECS, httpReplInternalConf),

    // Get User Credentials By User Id
    scn_wrapper(v20_get_user_credentials_for_user_id_scn, V20_GET_USER_CREDENTIALS_FOR_USER_ID_USERS_PER_SEC, MAX_DURATION_SECS, httpMainExternalConf),
    scn_wrapper(v20_get_user_credentials_for_user_id_repl_scn, V20_GET_USER_CREDENTIALS_FOR_USER_ID_REPL_USERS_PER_SEC, MAX_DURATION_SECS, httpMainExternalConf),
    scn_wrapper(v20_get_user_credentials_for_user_id_internal_scn, V20_GET_USER_CREDENTIALS_FOR_USER_ID_INTERNAL_USERS_PER_SEC, MAX_DURATION_SECS, httpReplExternalConf),
    scn_wrapper(v20_get_user_credentials_for_user_id_internal_repl_scn, V20_GET_USER_CREDENTIALS_FOR_USER_ID_INTERNAL_REPL_USERS_PER_SEC, MAX_DURATION_SECS, httpReplInternalConf),

    // List Global Roles By User Id
    scn_wrapper(v20_list_global_roles_for_user_id_scn, V20_LIST_GLOBAL_ROLES_FOR_USER_ID_USERS_PER_SEC, MAX_DURATION_SECS, httpMainExternalConf),
    scn_wrapper(v20_list_global_roles_for_user_id_repl_scn, V20_LIST_GLOBAL_ROLES_FOR_USER_ID_REPL_USERS_PER_SEC, MAX_DURATION_SECS, httpMainExternalConf),
    scn_wrapper(v20_list_global_roles_for_user_id_internal_scn, V20_LIST_GLOBAL_ROLES_FOR_USER_ID_INTERNAL_USERS_PER_SEC, MAX_DURATION_SECS, httpReplExternalConf),
    scn_wrapper(v20_list_global_roles_for_user_id_internal_repl_scn, V20_LIST_GLOBAL_ROLES_FOR_USER_ID_INTERNAL_REPL_USERS_PER_SEC, MAX_DURATION_SECS, httpReplInternalConf),

    scn_wrapper(v20_crud_user_scn, V20_CRUD_USER_USERS_PER_SEC, MAX_DURATION_SECS, httpMainExternalConf),

    scn_wrapper(v20_create_user_scn, V20_CREATE_USER_USERS_PER_SEC, MAX_DURATION_SECS, httpMainExternalConf),

    scn_wrapper(v20_list_all_roles_scn, V20_LIST_ROLES_FOR_IDENTITY_ADMIN_USERS_PER_SEC, MAX_DURATION_SECS, httpMainExternalConf)
//    scn_wrapper(v20_create_user_repl_scn, V20_CREATE_USER_REPL_USERS_PER_SEC, MAX_DURATION_SECS, httpMainExternalConf),
//    scn_wrapper(v20_create_user_internal_scn, V20_CREATE_USER_INTERNAL_USERS_PER_SEC, MAX_DURATION_SECS, httpReplExternalConf),
//    scn_wrapper(v20_create_user_internal_repl_scn, V20_CREATE_USER_INTERNAL_REPL_USERS_PER_SEC, MAX_DURATION_SECS, httpReplInternalConf)

//      scn_wrapper(create_da_scn, DAS_PER_SEC, MAX_DURATION_FOR_DAS_SECS/4, httpMainExternalConf, 0),
//       scn_wrapper(add_user_delegate_scn, DAS_PER_SEC, MAX_DURATION_FOR_DAS_SECS/4, httpMainExternalConf, MAX_DURATION_FOR_DAS_SECS/4),
//       scn_wrapper(create_nested_da_scn, DAS_PER_SEC, MAX_DURATION_FOR_DAS_SECS/4, httpMainExternalConf, 2 * MAX_DURATION_FOR_DAS_SECS/4),
//       scn_wrapper(delete_parent_da_scn, DAS_PER_SEC, MAX_DURATION_FOR_DAS_SECS/4, httpMainExternalConf, 3 * MAX_DURATION_FOR_DAS_SECS/4)

     )

} 
  setUp(list_scns():_*
  ).maxDuration(MAX_DURATION_SECS seconds)

 after {
   Identity.created_users_writer.flush()
   Identity.created_users_writer.close()
 }

}
