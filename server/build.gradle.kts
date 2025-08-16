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
    implementation("shared-lib:voicechat-common:1.0")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] to "org.server.Server"
    }
}