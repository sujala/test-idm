Feature: Cloud 1.1 Version
    Background:
        Given a auth 1.1 endpoint

    Scenario: Get Version Information
        When GET version call is made
        Then the response status should be 200