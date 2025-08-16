/*rootProject.name = "VoiceChat-Desktop"
println("toto")
dependencyResolutionManagement {
    versionCatalogs {
        create("sharedLibs") {
            library("common", ":shared:lib")
        }
    }
}
include(":shared:lib", ":client", ":server")
project(":shared:lib").projectDir = file("shared/lib")
*//*project(":shared:lib").buildFileName = "build.gradle.kts"*//*
println(project(":shared:lib").projectDir)*/
rootProject.name = "VoiceChat-Desktop"
include(":client")
include("server")