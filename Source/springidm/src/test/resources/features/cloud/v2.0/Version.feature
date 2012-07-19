Feature: Cloud 2.0 Version
    Background:
        Given a auth 2.0 endpoint

    Scenario: Get Version Information
        When GET version call is made
        Then the response status should be 200