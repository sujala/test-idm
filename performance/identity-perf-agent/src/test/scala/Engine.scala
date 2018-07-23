import io.gatling.app.Gatling
import io.gatling.core.config.GatlingPropertiesBuilder

object Engine extends App {

	val props = new GatlingPropertiesBuilder
	props.dataDirectory(IDEPathHelper.dataDirectory.toString)
	props.resultsDirectory(IDEPathHelper.resultsDirectory.toString)
	props.bodiesDirectory(IDEPathHelper.bodiesDirectory.toString)
	props.binariesDirectory(IDEPathHelper.mavenBinariesDirectory.toString)
	props.simulationClass("com.rackspacecloud.simulations.identity.IdentityConstantTputGenerateTokens")
//	props.simulationClass("com.rackspacecloud.simulations.identity.IdentityConstantTputUsersRampUp")
//	props.simulationClass("com.rackspacecloud.simulations.identity.IdentityDemo")
//	props.simulationClass("com.rackspacecloud.simulations.identity.DelegationAgreements")
//	props.simulationClass("com.rackspacecloud.simulations.identity.AddUserDelegateToDA")
//	props.simulationClass("com.rackspacecloud.simulations.identity.CreateNestedDAs")
//	props.simulationClass("com.rackspacecloud.simulations.identity.DeleteDelegationAgreements")
//	props.simulationClass("com.rackspacecloud.simulations.identity.DefaultUserAuthValidate")
//	props.simulationClass("com.rackspacecloud.simulations.identity.IdentityConstantTputDefaultUserDeletions")
//	props.simulationClass("com.rackspacecloud.simulations.identity.ListUsersInDomain")
//	props.simulationClass("com.rackspacecloud.simulations.identity.DomainUserDeletions")

	Gatling.fromMap(props.build)
}
