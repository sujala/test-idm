package com.rackspacecloud.scenarios

import com.rackspacecloud.api.identity.v2.ChangePasswordOfUserAcc
import io.gatling.core.Predef.{exec, scenario}

object IdentityChangePasswordOfUserAcc {
  val v20_change_password = scenario("V2.0_Change_Password").exec(ChangePasswordOfUserAcc.v20_change_password).exitHereIfFailed
}
