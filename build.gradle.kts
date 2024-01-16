import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    java
    application
    id("com.vaadin")
}

defaultTasks("clean", "build")

repositories {
    mavenCentral()
}

val vaadinVersion: String by extra

// add separate integration tests. Taken from
// https://docs.gradle.org/current/samples/sample_jvm_multi_project_with_additional_test_types.html
val integrationTest by sourceSets.creating

configurations[integrationTest.implementationConfigurationName].extendsFrom(configurations.testImplementation.get())
configurations[integrationTest.runtimeOnlyConfigurationName].extendsFrom(configurations.testRuntimeOnly.get())

val integrationTestTask = tasks.register<Test>("integrationTest") {
    description = "Runs integration tests."
    group = "verification"
    useJUnitPlatform()

    testClassesDirs = integrationTest.output.classesDirs
    classpath = configurations[integrationTest.runtimeClasspathConfigurationName] + integrationTest.output

    shouldRunAfter(tasks.test)
}


dependencies {
    // Vaadin
    implementation("com.vaadin:vaadin-core:$vaadinVersion") {
        afterEvaluate {
            if (vaadin.productionMode.get()) {
                exclude(module = "vaadin-dev")
            }
        }
    }

    // Vaadin-Boot
    implementation("com.github.mvysny.vaadin-boot:vaadin-boot:12.2")

    implementation("org.jetbrains:annotations:24.0.1")

    // logging
    // currently we are logging through the SLF4J API to SLF4J-Simple. See src/main/resources/simplelogger.properties file for the logger configuration
    implementation("org.slf4j:slf4j-simple:2.0.9")

    // Fast Vaadin unit-testing with Karibu-Testing: https://github.com/mvysny/karibu-testing
    testImplementation("com.github.mvysny.kaributesting:karibu-testing-v24:2.1.1")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Integration tests
    "integrationTestImplementation"(project)
    "integrationTestImplementation"("com.microsoft.playwright:playwright:1.40.0")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        exceptionFormat = TestExceptionFormat.FULL
    }
}

application {
    mainClass = "com.vaadin.starter.skeleton.Main"
}
