package com.rackspacecloud.scenarios

import com.rackspacecloud.api.identity.v2.{Tokens}
import io.gatling.core.Predef._

object DeleteParentDAs {
  val delete_parent_das = scenario("Delete Parent DA").exec(Tokens.v20_delete_delegation_agreement).exitHereIfFailed
}
