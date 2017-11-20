import io.gatling.app.Gatling
import io.gatling.core.config.GatlingPropertiesBuilder

object Engine extends App {

	val props = new GatlingPropertiesBuilder
	props.dataDirectory(IDEPathHelper.dataDirectory.toString)
	props.resultsDirectory(IDEPathHelper.resultsDirectory.toString)
	props.bodiesDirectory(IDEPathHelper.bodiesDirectory.toString)
	props.binariesDirectory(IDEPathHelper.mavenBinariesDirectory.toString)
	props.simulationClass("com.rackspacecloud.simulations.identity.IdentityConstantTputGenerateTokens")
//	props.simulationClass("com.rackspacecloud.simulations.identity.IdentityDemo")
//	props.simulationClass("com.rackspacecloud.simulations.identity.DefaultUserAuthValidate")
//	props.simulationClass("com.rackspacecloud.simulations.identity.IdentityConstantTputDefaultUserDeletions")

	Gatling.fromMap(props.build)
}
