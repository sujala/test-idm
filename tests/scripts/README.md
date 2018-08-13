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
* `LOG_PATH_LOCATION` - directory of log file.  Defaults to /opt

## Properties.json file

The properties json file consists of a list of properties with the
following attributes:

| Attribute | Required | Description |
| --------- | ------ | --- |
| defaultValue | no | default value returned for a property.  Provided by server |
| name | yes | property name |
| reloadable | yes | whether the property is reloadable or not.  Provided by server |
| description | yes | property description |
| source | yes | source file.  Provided by server |
| value | yes | property value. Provided by server |
| versionAdded | yes | version the property was added. Provied by server |
| multivalue | no | whether the string property is multivalue. If so, server returns it wrapped in an array |
| environment | no | local property that specifies which environment to compare the value to. Default is to compare it to every environment |
| action | no | local property that specifies a particular action to take on the property.  Currently, just ignore is supported |

## How to run it

```bash
docker build -t cid-compare .
docker run -ti --rm \
    -e Staging_DEVOPS_USER=user \
    -e Staging_DEVOPS_PASSWORD=pass \
    -e PROPERTY_FILE_LOCATION=/opt/properties.json \
    -e LOG_PATH_LOCATION=/var/log \
    -v $(pwd):/var/log \
    cid-compare:latest sh -c "python /opt/comparison.py | /usr/bin/jq ."
```

## If something fails, check out log file in `results.log` that's synched
to your local file
