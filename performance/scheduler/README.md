# Scheduler

Responsible for:

* Creating and viewing experiments (performance test profiles)
* Scheduling performance test executions based on a test profile
* Storing registered agents (those that are used for running a specific test)

Consists of:

* Falcon api.
  * Entry point is app.py, which sets up the routes for the scheduler api
  * Controllers module contains the api controllers that handle the validation and interraction with the data repositories
  * Repositories module encapsulates interraction with data modules
  * Data module contains all sql alchemy models used in the scheduler api
  * Tests/unit contains unit tests to test the scheduler api
