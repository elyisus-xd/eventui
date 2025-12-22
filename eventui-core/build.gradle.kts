plugins {
    `java-library`
}

dependencies {
    // Dependencia al módulo common
    api(project(":eventui-common"))

    // YAML parsing
    implementation("org.yaml:snakeyaml:2.2")

    // JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("org.assertj:assertj-core:3.24.2")
}
