# Prop Comparison Script

Configuration property comparison for staging and production environments

## Description

In order to validate that the configuration properties on deployed servers are
what we expect them to be, we need to pull down the property values and
compare them against the expected properties in our repository.

### This script consist of three files

* `comparison.py` runs the comparison.  The result is a JSON output that
  specifies the result status (`success` or `failure`) and an `error_list`
  list that shows the difference between local and remote property and the key
  they differ on.
* `properties.json` specifies the properties that are expected the be remotely.
  Due to the fact that some environments and regions may differ on the same
  property, optional `region` and `environment` keys may be specified to
  filter on the node.
* `environments.json` specifies the region, environment, endpoints and list
  of servers to compare against.

### The following environment variables should be provided to the script

* `DEVOPS_USER` - user id that is able to retrieve properties.
* `DEVOPS_PASSWORD` - user id that is able to retrieve properties.
* `PROPERTY_FILE_LOCATION` - location of `properties.json` file.  Defaults to
  current directory if not provided
* `ENVIRONMENT_FILE_LOCATION` - location of `environments.json` file.
  Defaults to current directory if not provided

## How to run it

```bash
docker build -t cid-compare .
docker run -ti --rm \
    -e Staging_DEVOPS_USER=user \
    -e Staging_DEVOPS_PASSWORD=pass \
    -e PROPERTY_FILE_LOCATION=/opt/properties.json \
    cid-compare:latest sh -c "python /opt/comparison.py | /usr/bin/jq ."
```
