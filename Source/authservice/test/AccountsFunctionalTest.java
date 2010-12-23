import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;

import play.mvc.Http;
import play.mvc.Http.*;
import play.test.Fixtures;
import play.test.FunctionalTest;

public class AccountsFunctionalTest extends FunctionalTest {
	private Pattern accountUrlPattern = Pattern.compile(".*/accounts/[0-9]+");
        private Request fakeRequest;

        @Before
        public void setup() {

            Fixtures.deleteAll();
            Fixtures.load("data.yml");

            fakeRequest = new Http.Request();

            // add headers
            Http.Header userAgentHeader = new Http.Header();
            userAgentHeader.name = "x-user-agent";
            userAgentHeader.values = Arrays.asList("huey");

            Http.Header signatureHeader = new Http.Header();
            signatureHeader.name = "x-api-signature";
            signatureHeader.values = Arrays.asList("asdfasdf");

            fakeRequest.headers.put("x-user-agent", userAgentHeader);
            fakeRequest.headers.put("x-api-signature", signatureHeader);
        }

	@Test
	public void shouldCreateAccount() {

                fakeRequest.url = "/accounts";
                fakeRequest.method = "POST";

		Response resp = POST(
				"/accounts",
				"application/json",
				"{\"username\":\"booyah\", \"password\":\"badpassword\", \"accountName\": \"wunderbar\", \"firstname\": \"foo\", \"middlename\":\"a\", \"lastname\":\"bar\", \"accountphone\": \"+15125550000\", \"accountMailingAddress\": \"1234 blah blah\", \"email\":\"foo.bar@example.org\", \"phone\":\"+15125550101\"}");
		assertStatus(201, resp);
		Matcher matcher = accountUrlPattern.matcher(resp.out.toString());
		assertTrue(matcher.find());
	}

	@Test
	public void shouldNotCreateBadAccount() {
		Response resp = POST("/accounts", "application/json", "{}");
		assertStatus(400, resp);
	}

        @Test
        public void shouldUpdateAccount(){
            Response resp = PUT("/accounts/1", "application/json", "{ \"accountName\": \"wunderbar\", \"accountphone\": \"+15125550000\", \"accountMailingAddress\": \"1234 blah blah\"}");
            assertStatus(200, resp);
        }

        @Test
        public void shouldNotUpdateAccount(){
            Response resp = PUT("/accounts/1", "application/json", "{}");
            assertStatus(400, resp);
        }

        @Test
        public void shouldNotUpdateUnknownAccount(){
            Response resp = PUT("/accounts/0", "application/json", "{ \"accountName\": \"wunderbar\", \"accountphone\": \"+15125550000\", \"accountMailingAddress\": \"1234 blah blah\"}");
            assertStatus(404, resp);
        }
}
