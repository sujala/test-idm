Cloud-Identity
--------------

The Rackspace Cloud Customer and Racker Identity System.

More info: [Cloud Identity One Rack Home](https://one.rackspace.com/display/auth/Home)

Team
-------------
* Werner Mendizabal
* Jorge Munoz
* Timothy Cline
* Robert Jacoby

Documentation
--------------

[API Developer Guide, v2.0](http://docs-internal.rackspace.com/auth/api/v2.0/auth-client-devguide/content/Overview-d1e65.html)

[API Admin Developer Guide, v2.0 ](http://docs-internal.rackspace.com/auth/api/v2.0/auth-admin-devguide/content/Overview-d1e65.html)

[API Developer Guide, v1.1](http://docs-internal.rackspace.com/auth/api/v1.1/auth-client-devguide/content/Overview-d1e65.html)

[API Admin Developer Guide, v1.1 (i)](http://docs-internal.rackspace.com/auth/api/v1.1/auth-admin-devguide/content/Overview-d1e65.html)

[Getting Started](https://one.rackspace.com/display/auth/Getting+Started)

[Development Quick-Start](https://one.rackspace.com/display/auth/Development+Quick-Start)

[Updating Identity's Documentation](https://one.rackspace.com/display/auth/Updating+Identity%27s+Documentation)

Dev Environment Setup
------------

[Setting up Global Auth on your laptop](https://one.rackspace.com/display/auth/Setting+up+Global+Auth+on+your+Laptop)

Johny (integration) Tests
-------------------------

Pre-requisites: docker, python2.7, pip

CD into `tests/` directory

Get all operations: `make help`

Run flake8: `make flake8`

Run tests via tox: `make test`

Run specific modules: `make API=api.v2.federation test_nose`

Run multiple times without cleaning up after every iteration: `make build && make API=api.v2.federation test_nose_no_build && ... && make clean`

Contributing
------------

1. Fork it.
2. Create a branch (`git checkout -b mybranch`)
3. Commit your changes (`git commit -am "Added Stuffs"`)
4. Push to the branch (`git push origin mybranch`)
5. Open a Pull Request
6. Enjoy a refreshing Diet Coke and wait


