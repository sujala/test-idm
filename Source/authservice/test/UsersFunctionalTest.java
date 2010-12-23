import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;

import play.mvc.Http.Response;
import play.test.FunctionalTest;

public class UsersFunctionalTest extends FunctionalTest {
	private Pattern userUrlPattern = Pattern.compile(".*/users/.+");

	private Response addUser() {
		Response resp = POST(
				"/users",
				"application/json",
				"{\"accountId\": \"1\", \"firstname\": \"foo\", \"middlename\":\"a\", \"lastname\":\"bar\", \"email\":\"foo.bar@example.org\", \"password\":\"badpassword\", \"username\":\"foo.bar\"}");
		return resp;
	}

	@Test
	public void shouldGetUserToken() {
		addUser();
		Response resp = GET("/users/foo.bar/token?password=badpassword");
		assertIsOk(resp);
	}

	@Test
	public void shouldNotGetBadUserToken() {
		addUser();
		Response resp = GET("/users/foo.bar/token?password=badpwd");
		assertIsNotFound(resp);
	}

	@Test
	public void shouldCreateUser() {
		Response resp = addUser();
		assertStatus(201, resp);
		Matcher matcher = userUrlPattern.matcher(resp.out.toString());
		assertTrue(matcher.find());
	}

	@Test
	public void shouldNotCreateBadUser() {
		Response resp = POST("/users", "application/json", "{}");
		assertStatus(400, resp);
	}


        @Test
        public void shouldUpdateUser(){
            Response resp = PUT("/users/foo.bar",
				"application/json",
				"{\"accountId\": \"1\", \"firstname\": \"foo\", \"middlename\":\"a\", \"lastname\":\"bar\", \"email\":\"foo.bar@example.org\", \"password\":\"badpassword\", \"username\":\"foo.bar\"}");
            assertStatus(200, resp);
        }

        @Test
        public void shouldNotUpdateUser(){
            Response resp = PUT("/users/foo.bar", "application/json", "{\"accountId\": \"1\", \"firstname\": \"foo\", \"middlename\":\"a\", \"lastname\":\"bar\", \"email\":\"foo.bar@example.org\", \"password\":\"\"}");
            assertStatus(400, resp);
        }

        @Test
        public void shouldNotUpdateUnknownUser(){
            Response resp = PUT("/users/xxxxxxx", "application/json", "{\"accountId\": \"1\", \"firstname\": \"foo\", \"middlename\":\"a\", \"lastname\":\"bar\", \"email\":\"foo.bar@example.org\", \"password\":\"badpassword\"}");
            assertStatus(404, resp);
        }
}
