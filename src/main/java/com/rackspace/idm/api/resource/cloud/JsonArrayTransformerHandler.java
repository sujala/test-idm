package com.rackspace.idm.api.resource.cloud;

/**
 * Our Json representation for arrays does not necessarily map one to one to the underlying jaxb object.
 * This class is responsible for helping to transform JSON arrays to JAXB objects and vice versa.
 *
 * For example,
 *
 * {
 *     "user" : {
 *         "firstname" : john,
 *         "roles" : [
 *             { "name" : managed },
 *             { "name" : moderator },
 *         ]
 *     }
 * }
 *
 * Does not map to jaxb object which is of form.
 *
 *      user.firstname = "john"
 *      user.roles.role = [ {"name" : managed}, {"name" : moderator} ]
 *
 * Jaxb object has an extra wrapper. The json payload that would have mapped directly would have been
 *
 *  {
 *     "user" : {
 *         "firstname" : john,
 *         "roles" : {
 *              "role" : [
 *                  { "name" : managed },
 *                  { "name" : moderator },
 *              ]
 *         }
 *     }
 * }
 *
 * This interface allows for each json reader/writer to define explicit instructions on whether a particular
 * array within JSON should be wrapped/unwrapped with this extraneous wrapper
 */
public interface JsonArrayTransformerHandler {
    boolean pluralizeJSONArrayWithName(String elementName);
}
