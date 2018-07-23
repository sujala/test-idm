package com.rackspacecloud.scenarios

import com.rackspacecloud.api.identity.v2.{Tokens}
import io.gatling.core.Predef._

object CreateNestedDAs {
  val create_nested_das = scenario("Create Nested DA").exec(Tokens.v20_create_nested_delegation_agreement).exitHereIfFailed
}
