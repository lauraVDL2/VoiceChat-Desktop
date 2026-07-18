plugins {
    java
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
}

repositories {
    mavenCentral()
    mavenLocal()
    // Custom repository
    maven {
        name = "henkelmax.public"
        url = uri("https://maven.maxhenkel.de/repository/public")
    }
}

val junitVersion = "5.12.1"

java {
    modularity.inferModulePath = false
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

javafx {
    version = "21.0.6"
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.web", "javafx.swing")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:unchecked")
}

application {
    mainClass.set("com.voicechat.client.VoiceChatApplication")
    applicationDefaultJvmArgs = listOf(
        "--add-modules", "javafx.controls,javafx.fxml,javafx.web,javafx.swing"
    )
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/lauraVDL2/VoiceChat-Common")
        credentials {
            username = System.getenv("COMMON_USERNAME")
            password = System.getenv("COMMON_TOKEN")
        }
    }
}

dependencies {
    implementation("shared-lib:voicechat-common:1.0.77")
    implementation("de.maxhenkel.opus4j:opus4j:2.1.3")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.14.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.2")
    implementation("io.github.resilience4j:resilience4j-all:1.7.0")
    implementation("org.apache.commons:commons-collections4:4.4")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("org.controlsfx:controlsfx:11.2.1")
    implementation("org.openjfx:javafx-fxml:17")
    implementation("org.openpnp:opencv:4.5.1-2")
    implementation("com.dlsc.formsfx:formsfx-core:11.6.0") {
        exclude(group = "org.openjfx")
    }
    implementation("org.kordamp.ikonli:ikonli-javafx:12.3.1")
    implementation("org.kordamp.bootstrapfx:bootstrapfx-core:0.4.0")
    implementation("eu.hansolo:tilesfx:21.0.9") {
        exclude(group = "org.openjfx")
    }
    testImplementation("org.junit.jupiter:junit-jupiter-api:${junitVersion}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${junitVersion}")
    testImplementation("org.testfx:testfx-junit5:4.0.18")
    testImplementation("org.mockito:mockito-core:4.8.0")
    testImplementation("org.hamcrest:hamcrest:2.1")
    implementation("org.openjfx:javafx-controls:21.0.6")
    implementation("org.openjfx:javafx-fxml:21.0.6")
    implementation("org.openjfx:javafx-web:21.0.6")
    implementation("org.openjfx:javafx-swing:21.0.6")
    implementation("org.neo4j:neo4j-ogm-core:4.0.19")
    implementation("org.neo4j:neo4j-ogm-bolt-driver:4.0.19")
    testImplementation("org.mockito:mockito-inline:4.8.0")
    testImplementation("org.neo4j.test:neo4j-harness:5.13.0")
}

// Test setup
tasks.test {
    useJUnitPlatform()
    jvmArgs = listOf(
        "--add-opens=javafx.graphics/com.sun.javafx.application=ALL-UNNAMED",
        "--add-opens=javafx.controls/javafx.scene=ALL-UNNAMED",
        "--add-opens=javafx.fxml/javafx.fxml=ALL-UNNAMED",
        "-Djava.awt.headless=true"
    )
    include("**/*Test.*")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf(
        "-Xlint:none" // disables all lint warnings
    ))
}