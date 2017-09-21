@Domain
Feature: Create Domain
    "Create a domain using the 2.0 Contract"
    Background:
        Given a auth 2.0 endpoint
        And a foundation endpoint
        And a valid Foundation-Api X-Auth-Token
        And a valid Identity-Admin X-Auth-Token
        And a valid Service-Admin X-Auth-Token

    Scenario: create domain with Identity-Admin
        Given a valid Identity-Admin X-Auth-Token
        When POST /domains call is made with:
            | enabled | description | domain name |
            |   false    | test-domain | RCN-000-000-111 |
            |   true     | test-domain | RCN-000-000-111 |
        Then the response should contain:
            | status | enabled | ID |
            | 200 | false | true |
            | 200 | true  | true |