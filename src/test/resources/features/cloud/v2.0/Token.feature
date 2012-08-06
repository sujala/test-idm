@token
Feature: Token
    Background:
        Given a auth 2.0 endpoint

    Scenario: Get Admin Token
        And the request type is "application/xml"
        When POST token call is made with the following data:
        |Username|Password |
        |auth    |Auth1234 |
        Then the response status should be 200
        And the body should be an access with:
        |attribute|
        |token|
        |user|
        |serviceCatelog|
        And the user element should be:
        | user_id | name   |
        | auth    | 173189 |
        And the user roles should be:
        | id | name           |
        | 1  | identity:admin |

