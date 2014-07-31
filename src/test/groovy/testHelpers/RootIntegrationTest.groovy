package testHelpers

import com.rackspace.idm.helpers.Cloud10Utils
import com.rackspace.idm.helpers.Cloud11Utils
import com.rackspace.idm.helpers.Cloud20Utils
import com.rackspace.idm.helpers.CloudTestUtils
import com.rackspace.idm.helpers.FoundationApiUtils
import com.sun.jersey.api.client.WebResource
import org.apache.commons.lang.math.RandomUtils
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification

import javax.ws.rs.core.MediaType

/**
 * Created with IntelliJ IDEA.
 * User: jacob
 * Date: 6/26/13
 * Time: 1:08 PM
 * To change this template use File | Settings | File Templates.
 */
@ContextConfiguration(locations = "classpath:app-config.xml")
class RootIntegrationTest extends Specification {

    @Autowired Cloud20Utils utils
    @Autowired Cloud11Utils utils11
    @Autowired Cloud10Utils utils10

    @Autowired CloudTestUtils testUtils

    @Autowired FoundationApiUtils foundationUtils

    @Shared double entropy
    @Shared int defaultExpirationSeconds

    @Shared WebResource resource

    @Shared def v1Factory  = new V1Factory()
    @Shared def v2Factory = new V2Factory()
    @Shared def entityFactory = new EntityFactory()
    @Shared def factory = new FoundationFactory()

    @Shared Cloud10Methods cloud10 = new Cloud10Methods()
    @Shared Cloud11Methods cloud11 = new Cloud11Methods()
    @Shared Cloud20Methods cloud20 = new Cloud20Methods()
    @Shared FoundationApiMethods foundation = new FoundationApiMethods()

    @Shared SingletonConfiguration staticIdmConfiguration = SingletonConfiguration.getInstance();

    def mediaTypeContext

    public setupSpec(){
        staticIdmConfiguration.reset()
        doSetupSpec()
        cloud20.init()
    }

    public cleanupSpec() {
        //reset the configuration properties at the end of each test class. This allows a test class to configure a common
        //configuration that applies to all tests in the class
        doCleanupSpec()
        staticIdmConfiguration.reset()
    }

    /**
     * Hook to allow subclasses to perform actions prior to the setupSpec in this base class. Spock will call the super class setupSpec method first, followed by subclasses.
     */
    def void doSetupSpec() {};

    /**
     * Hook to allow subclasses to perform actions after the cleanupSpec in this base class.
     */
    def void doCleanupSpec() {};

    def getRange(seconds) {
        HashMap<String, Date> range = new HashMap<>()
        def min = new DateTime().plusSeconds((int)Math.floor(seconds * (1 - entropy))).toDate()
        def max = new DateTime().plusSeconds((int)Math.ceil(seconds * (1 + entropy))).toDate()
        range.put("min", min)
        range.put("max", max)
        return range
    }

    def getRange(seconds, start, end) {
        HashMap<String, Date> range = new HashMap<>()
        def min = start.plusSeconds((int)Math.floor(seconds * (1 - entropy))).toDate()
        def max = end.plusSeconds((int)Math.ceil(seconds * (1 + entropy))).toDate()
        range.put("min", min)
        range.put("max", max)
        return range
    }

    def setLinkParams(List<String> links, Map<String, String[]> params) {
        for (String link : links) {
            def first = getFirstLink(link)
            if (first) {
                params.put("first", first)
                continue
            }
            def last = getLastLink(link)
            if (last) {
                params.put("last", last)
                continue
            }
            def prev = getPrevLink(link)
            if (prev) {
                params.put("prev", prev)
                continue
            }
            def next = getNextLink(link)
            if (next) {
                params.put("next", next)
                continue
            }
        }
    }

    def getFirstLink(String linkString) {
        def pattern = /marker=(.*)&limit=(.*)>; rel="first"/
        def matcher = (linkString =~ pattern)
        if (matcher) {
            return [ matcher[0][1], matcher[0][2] ]
        }
        return null
    }

    def getLastLink(String linkString) {
        def pattern = /marker=(.*)&limit=(.*)>; rel="last"/
        def matcher = (linkString =~ pattern)
        if (matcher) {
            return [ matcher[0][1], matcher[0][2] ]
        }
        return null
    }

    def getPrevLink(String linkString) {
        def pattern = /marker=(.*)&limit=(.*)>; rel="prev"/
        def matcher = (linkString =~ pattern)
        if (matcher) {
            return [ matcher[0][1], matcher[0][2] ]
        }
        return null
    }

    def getNextLink(String linkString) {
        def pattern = /marker=(.*)&limit=(.*)>; rel="next"/
        def matcher = (linkString =~ pattern)
        if (matcher) {
            return [ matcher[0][1], matcher[0][2] ]
        }
        return null
    }

    def getRandomNumber(Integer min, Integer max){
        Random r = new Random();
        return r.nextInt(max - min) + min;
    }

    def getRandomUUID(prefix='') {
        String.format("%s%s", prefix, UUID.randomUUID().toString().replace('-', ''))
    }

    def useMediaType(accept, contentType) {
        mediaTypeContext = GroovySpy(MediaTypeContext, global: true)
        mediaTypeContext.accept >> accept
        mediaTypeContext.contentType  >> contentType
    }

    def contentTypePermutations() {
        return [
                [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_XML_TYPE],
                [MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE],
                [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE],
                [MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_XML_TYPE]
        ]
    }

    static def getRandomIntegerInRange(int min, int max) {
        int randomNum = RandomUtils.nextInt((max - min) + 1) + min;
        return randomNum;
    }

    static def getRandomIntegerLessThan(int max) {
        return getRandomIntegerInRange(Integer.MIN_VALUE, max);
    }

    static def getRandomIntegerGreaterThan(int min) {
        return getRandomIntegerInRange(min, Integer.MAX_VALUE);
    }
}
