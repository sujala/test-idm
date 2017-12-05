package com.rackspacecloud.scenarios

import com.rackspacecloud.api.identity.v2.DomainUserDeletion

import io.gatling.core.Predef._

object IdentityDomainUserDeletion{

  val domain_user_deletion = exec(DomainUserDeletion.v20_user_deletion).exitHereIfFailed
  val domain_user_delete = scenario("V2.0_domain_user_delete_External").exec(domain_user_deletion)

}
