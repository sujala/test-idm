package testHelpers.saml

import com.rackspace.idm.Constants
import org.apache.commons.collections.CollectionUtils
import org.apache.commons.lang.StringUtils

/**
 * Created by robe4218 on 6/24/14.
 */
class SamlAttributeFactory {

    static HashMap<String, List<String>> createAttributes(String domain="1234", List<String> roles = Collections.EMPTY_LIST, String email=Constants.DEFAULT_FED_EMAIL) {
        HashMap<String, List<String>> attributes = new HashMap<String, List<String>>()
        if (StringUtils.isNotBlank(email)) {
            attributes.put("email", [email])
        }
        if (StringUtils.isNotBlank(domain)) {
            attributes.put("domain", [domain])
        }
        if (CollectionUtils.isNotEmpty(roles)) {
            attributes.put("roles", roles);
        }
        return attributes
    }


}
