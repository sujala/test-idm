package library;

import oauth.signpost.OAuthProviderListener;
import oauth.signpost.http.HttpRequest;
import oauth.signpost.http.HttpResponse;

/**
 * An example of how the user name and password could be passed along with the request for the OAuth request token.
 */
public class RsOAuthProviderListener implements OAuthProviderListener {

	private String userName;
	private String password;

	public RsOAuthProviderListener(String userName, String password) {
		this.userName = userName;
		this.password = password;
	}

	public void prepareRequest(HttpRequest request) throws Exception {
		//Append the user name and password to the request URL before the signature is generated.
		request.setRequestUrl(request.getRequestUrl() + "&username=\""
				+ userName + "\"&password=\"" + password + "\"");
	}

	public void prepareSubmission(HttpRequest request) throws Exception {
		// Do nothing
	}

	public boolean onResponseReceived(HttpRequest arg0, HttpResponse arg1)
			throws Exception {
		return false;
	}
}
