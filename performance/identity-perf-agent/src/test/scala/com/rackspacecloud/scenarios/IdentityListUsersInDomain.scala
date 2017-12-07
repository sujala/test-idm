package com.rackspacecloud.scenarios

import com.rackspacecloud.api.identity.v2.TokensListUsersInDomain
import io.gatling.core.Predef.{exec, scenario}

object IdentityListUsersInDomain {
  val v20_list_users_in_a_domain = scenario("V2.0_List_Users_In_Domain_Id").exec(TokensListUsersInDomain.v20_list_users_in_a_domain).exitHereIfFailed
}
