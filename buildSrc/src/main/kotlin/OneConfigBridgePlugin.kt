import org.gradle.api.Plugin
import org.gradle.api.Project

class OneConfigBridgePlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")

        configurations.create("modApi") {
            configurations.named("api") {
                extendsFrom(this@create)
            }
        }
        configurations.create("modImplementation") {
            configurations.named("implementation") {
                extendsFrom(this@create)
            }
        }
        configurations.create("modRuntimeOnly") {
            configurations.named("runtimeOnly") {
                extendsFrom(this@create)
            }
        }
        configurations.create("modCompileOnly") {
            configurations.named("compileOnly") {
                extendsFrom(this@create)
            }
        }
    }
}
