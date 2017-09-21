package testHelpers

import com.rackspace.idm.api.resource.cloud.email.EmailTemplateConstants
import org.apache.commons.lang.StringUtils

import javax.mail.internet.MimeMessage

class EmailUtils {
    /**
     * For testing purposes dynamic values that can be injected into template will be listed in PropertyFile format in
     * the default email content.
     *
     * @param message
     * @return
     */
    def static Map<String, String> extractDynamicPropsFromDefaultEmail(MimeMessage message) {
        String raw = StringUtils.trim(message.content)
        Map<String, String> map = new HashMap<>();
        raw.splitEachLine("=") { items ->
            map.put(items[0], items[1])
        }
        return map;
    }

    def static  extractTokenFromDefaultForgotPasswordEmail(MimeMessage message) {
        def map = extractDynamicPropsFromDefaultEmail(message)
        return StringUtils.trim(map.get(EmailTemplateConstants.FORGOT_PASSWORD_TOKEN_STRING_PROP));
    }

}
