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
    implementation("shared-lib:voicechat-common:1.0.9")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.2")
    implementation("org.neo4j:neo4j-ogm-core:4.0.19")
    implementation("org.neo4j:neo4j-ogm-bolt-driver:4.0.19")
    implementation("ch.qos.logback:logback-classic:1.2.11")
    implementation("org.mindrot:jbcrypt:0.4")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] to "org.server.Server"
    }
}