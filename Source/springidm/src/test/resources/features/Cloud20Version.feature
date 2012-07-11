Feature: Cloud 20 Version

    Scenario: Get Version Information
        Given a auth 1.0 endpoint
        When GET version call is made
        Then the response status should be 200