package com.rackspacecloud.scenarios

import com.rackspacecloud.api.identity.v2.Tokens
import io.gatling.core.Predef._

object AddUserDelegate {
  val add_user_delegate = scenario("Add User Delegate To DA").exec(Tokens.v20_add_user_delegate).exitHereIfFailed
}
