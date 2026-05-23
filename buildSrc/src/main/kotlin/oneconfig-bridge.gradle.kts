plugins {
    kotlin("jvm")
}

configurations.create("modImplementation") {
    extendsFrom(configurations.named("implementation"))
}
configurations.create("modRuntimeOnly") {
    extendsFrom(configurations.named("runtimeOnly"))
}
configurations.create("modCompileOnly") {
    extendsFrom(configurations.named("compileOnly"))
}
