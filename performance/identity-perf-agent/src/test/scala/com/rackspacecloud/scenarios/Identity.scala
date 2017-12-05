package com.rackspacecloud.scenarios

import com.rackspacecloud.api.identity.v2.Tokens

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.commons.validation._

import scala.util.Random
import scala.concurrent.duration._
import com.rackspacecloud.Options
import java.io._
import com.typesafe.config.ConfigFactory

object Identity {
  val conf = ConfigFactory.load()

  var DATA_LOCATION : String = ""
  try{
    DATA_LOCATION = conf.getString("data_dir")
  } catch {
    case missing : Throwable=> println("Missing data dir, using default")
  }
 
   // For Create User
   var created_users_writer = new PrintWriter(new File(DATA_LOCATION + "data/identity/created_users.dat"))

   // V11 Authenticate
   val v11_apikey_auth                 = scenario("V1.1_Authenticate_External").exec(Tokens.v11_authenticate).exitHereIfFailed
   val v11_apikey_auth_repl            = scenario("V1.1_Authenticate_External_Replication").exec(Tokens.v11_authenticate).exitHereIfFailed

  // V20 Authenticate
  val v20_apikey_auth                  = scenario("V2.0_Authenticate_External").exec(Tokens.v20_authenticate).exitHereIfFailed
  val v20_apikey_auth_rcn_roles        = scenario("V2.0_Authenticate_External_RCN_Roles").exec(Tokens.v20_authenticate_rcn_roles).exitHereIfFailed

  val v20_apikey_auth_default_user    = scenario("V2.0_Authenticate_Default_Users_External").exec(Tokens.v20_authenticate_default_user).exitHereIfFailed

  val v20_apikey_auth_repl             = scenario("V2.0_Authenticate_External_Replication").exec(Tokens.v20_authenticate).exitHereIfFailed
  val v20_apikey_auth_internal         = scenario("V2.0_Authenticate_Internal").exec(Tokens.v20_authenticate).exitHereIfFailed
  val v20_apikey_auth_internal_repl    = scenario("V2.0_Authenticate_Internal_Replication").exec(Tokens.v20_authenticate).exitHereIfFailed

  // V20 SAML Authenticate
  val v20_saml_auth                  = scenario("V2.0_SAML_Authenticate_External").exec(Tokens.v20_saml_authenticate).exitHereIfFailed
  val v20_saml_auth_repl             = scenario("V2.0_SAML_Authenticate_External_Replication").exec(Tokens.v20_saml_authenticate).exitHereIfFailed
  val v20_saml_auth_internal         = scenario("V2.0_SAML_Authenticate_Internal").exec(Tokens.v20_saml_authenticate).exitHereIfFailed
  val v20_saml_auth_internal_repl    = scenario("V2.0_SAML_Authenticate_Internal_Replication").exec(Tokens.v20_saml_authenticate).exitHereIfFailed

  // V20 Impersonate
  val v20_impersonate                  = scenario("V2.0_Impersonate_External").exec(Tokens.v20_impersonate).exitHereIfFailed
  val v20_impersonate_repl             = scenario("V2.0_Impersonate_External_Replication").exec(Tokens.v20_impersonate).exitHereIfFailed
  val v20_impersonate_internal         = scenario("V2.0_Impersonate_Internal").exec(Tokens.v20_impersonate).exitHereIfFailed
  val v20_impersonate_internal_repl    = scenario("V2.0_Impersonate_Internal_Replication").exec(Tokens.v20_impersonate).exitHereIfFailed


  // V20 Validate
  val v20_token_validate               = scenario("V2.0_Validate").exec(Tokens.v20_validate).exitHereIfFailed 
  val v20_token_validate_rcn_roles     = scenario("V2.0_Validate_RCN_Roles").exec(Tokens.v20_validate_rcn_roles).exitHereIfFailed 
  val v20_token_validate_repl          = scenario("V2.0_Validate_Replication").exec(Tokens.v20_validate).exitHereIfFailed
  val v20_token_validate_internal      = scenario("V2.0_Validate_Internal").exec(Tokens.v20_validate).exitHereIfFailed
  val v20_token_validate_internal_repl = scenario("V2.0_Validate_Internal_Replication").exec(Tokens.v20_validate).exitHereIfFailed

  // V20 Validate Default User's token
  val v20_token_validate_default_user        = scenario("V2.0_Validate_Default_User").exec(Tokens.v20_validate_default_user).exitHereIfFailed

  // V11 Get User By Mosso Id
  val v11_get_user_by_mosso_id               = scenario("V1.1_Get_User_By_Mosso_Id").exec(Tokens.v11_get_user_by_mosso_id).exitHereIfFailed
  val v11_get_user_by_mosso_id_repl          = scenario("V1.1_Get_User_By_Mosso_Id_Replication").exec(Tokens.v11_get_user_by_mosso_id).exitHereIfFailed
  val v11_get_user_by_mosso_id_internal      = scenario("V1.1_Get_User_By_Mosso_Id_Internal").exec(Tokens.v11_get_user_by_mosso_id).exitHereIfFailed
  val v11_get_user_by_mosso_id_internal_repl = scenario("V1.1_Get_User_By_Mosso_Id_Internal_Replication").exec(Tokens.v11_get_user_by_mosso_id).exitHereIfFailed

// V11 Get Domain By Mosso Id
  val v11_get_user_in_domain_by_mosso_id               = scenario("V1.1_Get_User_In_Domain_By_Mosso_Id").exec(Tokens.v11_get_user_in_domain_by_mosso_id).exitHereIfFailed
  val v11_get_user_in_domain_by_mosso_id_repl          = scenario("V1.1_Get_User_In_Domain_By_Mosso_Id_Replication").exec(Tokens.v11_get_user_in_domain_by_mosso_id).exitHereIfFailed
  val v11_get_user_in_domain_by_mosso_id_internal      = scenario("V1.1_Get_User_In_Domain_By_Mosso_Id_Internal").exec(Tokens.v11_get_user_in_domain_by_mosso_id).exitHereIfFailed
  val v11_get_user_in_domain_by_mosso_id_internal_repl = scenario("V1.1_Get_User_In_Domain_By_Mosso_Id_Internal_Replication").exec(Tokens.v11_get_user_in_domain_by_mosso_id).exitHereIfFailed

  // V11 Get User By Nast Id
  val v11_get_user_by_nast_id               = scenario("V1.1_Get_User_By_Nast_Id").exec(Tokens.v11_get_user_by_nast_id).exitHereIfFailed
  val v11_get_user_by_nast_id_repl          = scenario("V1.1_Get_User_By_Nast_Id_Replication").exec(Tokens.v11_get_user_by_nast_id).exitHereIfFailed
  val v11_get_user_by_nast_id_internal      = scenario("V1.1_Get_User_By_Nast_Id_Internal").exec(Tokens.v11_get_user_by_nast_id).exitHereIfFailed
  val v11_get_user_by_nast_id_internal_repl = scenario("V1.1_Get_User_By_Nast_Id_Internal_Replication").exec(Tokens.v11_get_user_by_nast_id).exitHereIfFailed

  // V11 Get User By Name
  val v11_get_user_by_name               = scenario("V1.1_Get_User_By_Name").exec(Tokens.v11_get_user_by_name).exitHereIfFailed 
  val v11_get_user_by_name_repl          = scenario("V1.1_Get_User_By_Name_Replication").exec(Tokens.v11_get_user_by_name).exitHereIfFailed
  val v11_get_user_by_name_internal      = scenario("V1.1_Get_User_By_Name_Internal").exec(Tokens.v11_get_user_by_name).exitHereIfFailed
  val v11_get_user_by_name_internal_repl = scenario("V1.1_Get_User_By_Name_Internal_Replication").exec(Tokens.v11_get_user_by_name).exitHereIfFailed

 // V20 List Groups For User Id
 val v20_list_groups_for_user_id               = scenario("V2.0_List_Groups_For_User_Id").exec(Tokens.v20_list_groups_for_user_id).exitHereIfFailed
  val v20_list_groups_for_user_id_repl          = scenario("V2.0_List_Groups_For_User_Id_Replication").exec(Tokens.v20_list_groups_for_user_id).exitHereIfFailed
  val v20_list_groups_for_user_id_internal      = scenario("V2.0_List_Groups_For_User_Id_Internal").exec(Tokens.v20_list_groups_for_user_id).exitHereIfFailed
  val v20_list_groups_for_user_id_internal_repl = scenario("V2.0_List_Groups_For_User_Id_Internal_Replication").exec(Tokens.v20_list_groups_for_user_id).exitHereIfFailed

  // V20 Get User Credentials 
 val v20_get_user_credentials_for_user_id               = scenario("V2.0_Get_User_Credentials_For_User_Id").exec(Tokens.v20_get_user_credentials_for_user_id).exitHereIfFailed
  val v20_get_user_credentials_for_user_id_repl          = scenario("V2.0_Get_User_Credentials_For_User_Id_Replication").exec(Tokens.v20_get_user_credentials_for_user_id).exitHereIfFailed
  val v20_get_user_credentials_for_user_id_internal      = scenario("V2.0_Get_User_Credentials_For_User_Id_Internal").exec(Tokens.v20_get_user_credentials_for_user_id).exitHereIfFailed
  val v20_get_user_credentials_for_user_id_internal_repl = scenario("V2.0_Get_User_Credentials_For_User_Id_Internal_Replication").exec(Tokens.v20_get_user_credentials_for_user_id).exitHereIfFailed

  // V20 List Global Roles 
 val v20_list_global_roles_for_user_id               = scenario("V2.0_List_Global_Roles_For_User_Id").exec(Tokens.v20_list_global_roles_for_user_id).exitHereIfFailed
  val v20_list_global_roles_for_user_id_repl          = scenario("V2.0_List_Global_Roles_For_User_Id_Replication").exec(Tokens.v20_list_global_roles_for_user_id).exitHereIfFailed
  val v20_list_global_roles_for_user_id_internal      = scenario("V2.0_List_Global_Roles_For_User_Id_Internal").exec(Tokens.v20_list_global_roles_for_user_id).exitHereIfFailed
  val v20_list_global_roles_for_user_id_internal_repl = scenario("V2.0_List_Global_Roles_For_User_Id_Internal_Replication").exec(Tokens.v20_list_global_roles_for_user_id).exitHereIfFailed

  val v20_list_users_in_a_domain = scenario("V2.0_List_Users_In_Domain_Id").exec(Tokens.v20_list_users_in_a_domain).exitHereIfFailed

def write_created_user(session:Session):Session = {
   println(s"Create user, writing " + session("admin_token").as[String] + "," + session("user_id").as[String] + "")
   created_users_writer.write(
       session("admin_token").as[String] + "," +
       session("user_id").as[String] + "," +
       session("per_stg_1_${next_int}").as[String] + 
       "\n"); session
}
  // V20 
 val v20_create_user               = scenario("V2.0_Create_User").exec(Tokens.v20_create_user).exec { session=> write_created_user(session)}.exitHereIfFailed
  val v20_create_user_repl          = scenario("V2.0_Create_User_Replication").exec(Tokens.v20_create_user).exec { session=> write_created_user(session)}.exitHereIfFailed
  val v20_create_user_internal      = scenario("V2.0_Create_User_Internal").exec(Tokens.v20_create_user).exec { session=> write_created_user(session)}.exitHereIfFailed
  val v20_create_user_internal_repl = scenario("V2.0_Create_User_Internal_Replication").exec(Tokens.v20_create_user).exec { session=> write_created_user(session)}.exitHereIfFailed


}
