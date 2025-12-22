plugins {
    id("fabric-loom")
}

dependencies {
    minecraft("com.mojang:minecraft:1.21.1")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:0.16.9")
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.108.0+1.21.1")

    // Dependencias internas del proyecto
    implementation(project(":eventui-core"))
    include(project(":eventui-core"))

    implementation(project(":eventui-common"))
    include(project(":eventui-common"))

    // Include SnakeYAML para runtime
    include("org.yaml:snakeyaml:2.2")
}

tasks.processResources {
    inputs.property("version", project.version)

    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

java {
    withSourcesJar()
}
