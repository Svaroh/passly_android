import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.provider.Provider

class QualityDependencies(
    val ktlintComposeRuleset: Provider<MinimalExternalModuleDependency>,
    val detektComposeRuleset: Provider<MinimalExternalModuleDependency>,
)
