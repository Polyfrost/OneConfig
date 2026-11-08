import org.gradle.api.Plugin
import org.gradle.api.Project

class OneConfigNeoForgePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.apply(OneConfigBridgePlugin::class.java)
        target.pluginManager.apply(OneConfigSetupPlugin::class.java)
    }
}
