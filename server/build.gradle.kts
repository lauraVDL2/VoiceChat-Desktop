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
    implementation("shared-lib:voicechat-common:1.0.35")
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
}

tasks.jar {
    manifest {
        attributes["Main-Class"] to "org.server.Server"
    }
}