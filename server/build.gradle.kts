plugins {
    java
    application
}

// Specify the main class
application {
    mainClass.set("org.server.Server")
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
    implementation("shared-lib:voicechat-common:1.0.59")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.2")
    implementation("org.apache.commons:commons-collections4:4.4")
    implementation("org.neo4j:neo4j-ogm-core:4.0.19")
    implementation("org.neo4j:neo4j-ogm-bolt-driver:4.0.19")
    implementation("ch.qos.logback:logback-classic:1.5.13")
    implementation("ch.qos.logback:logback-core:1.5.13")
    implementation("org.slf4j:slf4j-simple:2.0.16")
    implementation("org.slf4j:slf4j-api:2.0.16")
    implementation("org.mindrot:jbcrypt:0.4")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.14.0")
    implementation("com.microsoft.graph:microsoft-graph:5.25.0") // Check for latest version
    implementation("com.microsoft.graph:microsoft-graph-core:2.0.15")
    implementation("com.azure:azure-identity:1.4.4") // For OAuth2 authentication
    implementation("io.netty:netty-transport-native-epoll:4.1.92.Final:linux-x86_64")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito:mockito-core:4.8.0")
    testImplementation("org.neo4j.test:neo4j-harness:5.13.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes["Main-Class"] to "org.server.Server"
    }
}