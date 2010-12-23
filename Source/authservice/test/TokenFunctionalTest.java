import org.junit.Test;

import play.mvc.Http.Response;
import play.test.FunctionalTest;

public class TokenFunctionalTest extends FunctionalTest {

	private String makeToken() {
		Response resp = POST(
				"/users",
				"application/json",
				"{\"accountId\": \"1\", \"firstname\": \"foo\", \"middlename\":\"a\", \"lastname\":\"bar\", \"email\":\"foo.bar@example.org\", \"password\":\"badpassword\", \"username\":\"foo.bar\"}");

		resp = GET("/users/foo.bar/token?password=badpassword");
		String token = resp.out.toString().trim();
		return token;
	}

	@Test
	public void shouldValidateUserToken() {
		String token = makeToken();

		Response resp = GET("/tokens/" + token + "?username=foo.bar");
		assertIsOk(resp);
	}

	@Test
	public void shouldErrorOnInvalidToken() {
		String token = makeToken();
		Response resp = GET("/tokens/" + token + "?username=baduser");
		assertStatus(500, resp);
		assertTrue(resp.out.toString().contains("Invalid or expired token"));
	}
	
	@Test
	public void shouldErrorOnExpiredToken() {
		String token = makeToken();
		sleep(65);
		Response resp = GET("/tokens/" + token + "?username=badpassword");
		assertStatus(500, resp);
		assertTrue(resp.out.toString().contains("Invalid or expired token"));
	}
}
