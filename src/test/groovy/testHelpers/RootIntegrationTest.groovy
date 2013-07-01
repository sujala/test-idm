package testHelpers

import com.sun.jersey.api.client.WebResource
import org.joda.time.DateTime
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification

import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.ensureGrizzlyStarted

/**
 * Created with IntelliJ IDEA.
 * User: jacob
 * Date: 6/26/13
 * Time: 1:08 PM
 * To change this template use File | Settings | File Templates.
 */
@ContextConfiguration(locations = "classpath:app-config.xml")
class RootIntegrationTest extends Specification {

    @Shared double entropy
    @Shared int defaultExpirationSeconds

    @Shared WebResource resource

    @Shared def v1Factory  = new V1Factory()
    @Shared def v2Factory = new V2Factory()
    @Shared def entityFactory = new EntityFactory()

    @Shared Cloud20Methods cloud20 = new Cloud20Methods()
    @Shared Cloud11Methods cloud11 = new Cloud11Methods()
    @Shared Cloud10Methods cloud10 = new Cloud10Methods()

    public setupSpec(){
        this.resource = ensureGrizzlyStarted("classpath:app-config.xml")

        cloud20.setResource(resource)
        cloud11.setResource(resource)
        cloud10.setResource(resource)
    }

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
}
