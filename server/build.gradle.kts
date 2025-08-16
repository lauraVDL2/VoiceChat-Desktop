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
            username = "lauraVDL2"
            password = "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"
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