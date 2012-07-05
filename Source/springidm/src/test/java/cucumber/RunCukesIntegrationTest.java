package cucumber;

import cucumber.junit.Cucumber;
import org.junit.runner.RunWith;


/**
* User: alan.erwin
* Date: 6/8/12
* Time: 2:08 PM
*/
@RunWith(Cucumber.class)
@Cucumber.Options(glue = ("src/test/groovy/step_definitions"), format = {"pretty", "html:target/cucumber-html-report"}, features = "src/test/resources/features")
public class RunCukesIntegrationTest {

}
