plugins {
    java
    application
    id("org.javamodularity.moduleplugin") version "1.8.15"
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("org.beryx.jlink") version "2.25.0"
}

repositories {
    mavenCentral()
    mavenLocal()
}

val junitVersion = "5.12.1"

java {
    modularity.inferModulePath = true
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

application {
    mainModule.set("com.voicechat.client")
    mainClass.set("com.voicechat.client.VoiceChatApplication")
}

javafx {
    version = "21.0.6"
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.web", "javafx.swing")
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
    implementation("shared-lib:voicechat-common:1.0.11")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.2")
    implementation("io.github.resilience4j:resilience4j-all:1.7.0")
    /*implementation("io.github.resilience4j:resilience4j-bom:1.7.0")*/
    implementation("org.controlsfx:controlsfx:11.2.1")
    implementation("org.openjfx:javafx-fxml:17")
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
}

tasks.withType<Test> {
    useJUnitPlatform()
}

jlink {
    imageZip.set(layout.buildDirectory.file("/distributions/app-${javafx.platform.classifier}.zip"))
    options.set(listOf("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages"))
    launcher {
        name = "app"
    }
}