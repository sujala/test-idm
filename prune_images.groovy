import java.text.SimpleDateFormat
import java.util.Date
import org.joda.time.DateTime

/**
 * This job prunes images generated Identity's CICD.
 */

def runBuild(scm) {
    library "tesla@v0.8.2"

    stage('Prune Images') {
        node('master') {
            openshift.withCluster {
                openshift.withProject('customer-identity-cicd') {
                    def images = openshift.selector('istag')

                    images.withEach {
                        def name = it.object().metadata.name
                        def creationTimestamp = it.object().metadata.creationTimestamp

                        // Generate a date from the creation timestamp
                        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                        Date date = formatter.parse(creationTimestamp)

                        // Check if image is older then 24 hours
                        DateTime dateTime = new DateTime(date)
                        if (dateTime.isBefore(DateTime.now().minusDays(1))) {
                            echo "deleting image - ${name}"
                            openshift.selector('istag', name).delete()
                        } else {
                            echo "not deleting image - ${name}"
                        }
                    }
                }
            }
        }
    }
}

return this
