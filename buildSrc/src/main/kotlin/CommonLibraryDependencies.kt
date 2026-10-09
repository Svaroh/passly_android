import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.provider.Provider

class CommonLibraryDependencies(
    val kotlinStdlib: Provider<MinimalExternalModuleDependency>,
    val kotlinCoroutines: Provider<MinimalExternalModuleDependency>,
    val timber: Provider<MinimalExternalModuleDependency>,
    val junit: Provider<MinimalExternalModuleDependency>,
    val mockitoKotlin: Provider<MinimalExternalModuleDependency>,
    val truth: Provider<MinimalExternalModuleDependency>,
    val turbine: Provider<MinimalExternalModuleDependency>,
    val kotlinCoroutinesTest: Provider<MinimalExternalModuleDependency>,
    val koinBom: Provider<MinimalExternalModuleDependency>,
    val koinTest: Provider<MinimalExternalModuleDependency>,
    val koinTestJunit: Provider<MinimalExternalModuleDependency>,
    val androidxJunit: Provider<MinimalExternalModuleDependency>,
    val desugarJdkLibs: Provider<MinimalExternalModuleDependency>,
)
