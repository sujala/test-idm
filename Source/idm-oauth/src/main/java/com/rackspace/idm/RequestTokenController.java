package com.rackspace.idm;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthMessage;
import net.oauth.server.OAuthServlet;

import org.slf4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Handles requests for the application welcome page.
 */
@Controller
public class RequestTokenController {

	public RequestTokenController() {
		try {
			SampleOAuthProvider.loadConsumers();
		} catch (IOException e) {
			throw new IllegalStateException(
					"Failed to initialize the consumer data:\n"
							+ e.getMessage());
		}
	}

	private Logger logger = org.slf4j.LoggerFactory
			.getLogger(RequestTokenController.class);

	@RequestMapping(value = "request_token", method = RequestMethod.POST)
	public void getRequestToken(HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException {
		logger.info("Processing request token");
		
		try {
			OAuthMessage requestMessage = OAuthServlet
					.getMessage(request, null);

			OAuthConsumer consumer = SampleOAuthProvider
					.getConsumer(requestMessage);

			OAuthAccessor accessor = new OAuthAccessor(consumer);
			SampleOAuthProvider.VALIDATOR.validateMessage(requestMessage,
					accessor);
			{
				// Support the 'Variable Accessor Secret' extension
				// described in http://oauth.pbwiki.com/AccessorSecret
				String secret = requestMessage
						.getParameter("oauth_accessor_secret");
				if (secret != null) {
					accessor.setProperty(OAuthConsumer.ACCESSOR_SECRET, secret);
				}
			}
			// generate request_token and secret
			SampleOAuthProvider.generateRequestToken(accessor);

			response.setContentType("text/plain");
			OutputStream out = response.getOutputStream();
			OAuth.formEncode(OAuth.newList("oauth_token",
					accessor.requestToken, "oauth_token_secret",
					accessor.tokenSecret), out);
			out.close();

		} catch (Exception e) {
			SampleOAuthProvider.handleException(e, request, response, true);
		}
	}
}
