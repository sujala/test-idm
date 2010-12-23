package controllers;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
import play.mvc.Controller;

/**
 * An example using the Signpost client library
 *
 */
public class OauthTest extends Controller {

	public static void test() throws IOException, OAuthMessageSignerException,
			OAuthNotAuthorizedException, OAuthExpectationFailedException,
			OAuthCommunicationException {
		InputStream in = Object.class.getResourceAsStream("/oauth.properties");
		Properties props = new Properties();
		props.load(in);

		// create a consumer object and configure it with the access
		// token and token secret obtained from the service provider
		OAuthConsumer consumer = new DefaultOAuthConsumer(props
				.getProperty("consumer.key"), props
				.getProperty("consumer.secret"));

		// create a new service provider object and configure it with
		// the URLs which provide request tokens, access tokens, and
		// the URL to which users are sent in order to grant permission
		// to your application to access protected resources
		OAuthProvider provider = new DefaultOAuthProvider(props
				.getProperty("request.token.url"), props
				.getProperty("access.token.url"), props
				.getProperty("authorize.url"));

		// fetches a request token from the service provider and builds
		// a url based on AUTHORIZE_WEBSITE_URL and CALLBACK_URL to
		// which your app must now send the user
		String url = provider.retrieveRequestToken(consumer, props
				.getProperty("callback.url"));

		System.out.println(url);
		
		redirect(url);
	}

	public static void callback(String oauth_token) {
		renderText("Called back with oauth_token:" + oauth_token);
	}
}
