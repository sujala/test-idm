package com.rackspacecloud.scenarios

import com.rackspacecloud.api.identity.v2.DefaultUserDeletion
import io.gatling.core.Predef.{exec, scenario}

object IdentityDefaultUserDeletion {
  val default_user_deletion = exec(DefaultUserDeletion.v20_default_user_deletion).exitHereIfFailed
  val default_user_delete = scenario("V2.0_Default_User_Delete_External").exec(default_user_deletion)

}
