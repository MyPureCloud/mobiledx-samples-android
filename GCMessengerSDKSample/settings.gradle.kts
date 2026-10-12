pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        if (!providers.gradleProperty("useMavenCentral").getOrElse("false").toBoolean()) {
            maven { url = uri("https://genesysdx.jfrog.io/artifactory/genesys-cloud-android.prod/") }
        }
    }
    versionCatalogs {
        // sample-app/app/build.gradle.kts references "sampleLibs" so the same build file works
        // both standalone (this settings file) and as part of the root project, where "libs"
        // already points at the root catalog and "sampleLibs" is the alias for this one.
        create("sampleLibs") {
            from(files("gradle/libs.versions.toml"))
        }
    }
}
rootProject.name = "GCMessengerSDKSample"
include(":app")
