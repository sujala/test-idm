Feature: Global Auth Authentication

    Scenario: Get Admin Token
      Given a auth 2.0 endpoint
      And the request type is "application/xml"
      When POST call is made to "/v2.0/tokens"
      Then the response status should be 200
