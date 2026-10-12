import java.util.Properties

plugins {
    id("com.android.application")
    alias(libs.plugins.kotlin.compose)
}

val oktaProperties = Properties().apply {
    val oktaPropertiesFile = File(project.projectDir, "okta.properties")
    if (oktaPropertiesFile.exists()) {
        load(oktaPropertiesFile.reader())
        println("okta.properties loaded.")
    } else {
        println("okta.properties does not exist in the path and will be ignored.")
    }
}

val googleServicesFile = File(project.projectDir, "google-services.json")
val isGoogleServicesFileExists = googleServicesFile.exists()

// project version section
val projectVersion = (rootProject.findProperty("versionParam") ?: "2.6.0").toString()
val useArtifactory = (rootProject.findProperty("useArtifactory") as? String)?.toBoolean() ?: (rootProject.name != "mobile-messenger-android-sdk")
val useMavenCentral = (rootProject.findProperty("useMavenCentral") as? String)?.toBoolean() ?: false
val injectedVersionCode = project.findProperty("versionCode")?.toString()?.toInt()
// project version section

if (isGoogleServicesFileExists) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
    println("Google Services plugin enabled.")
    println("Crashlytics plugin enabled.")
}

android {
    namespace = "com.genesys.cloud.messenger.sample"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.genesys.cloud.messenger.sample"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = injectedVersionCode ?: 1
        versionName = projectVersion

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // OKTA
        buildConfigField("String", "OKTA_DOMAIN", "\"" + oktaProperties["oktaDomain"] + "\"")
        buildConfigField("String", "CLIENT_ID", "\"" + oktaProperties["clientId"] + "\"")
        buildConfigField("String", "SIGN_IN_REDIRECT_URI", "\"" + oktaProperties["signInRedirectUri"] + "\"")
        buildConfigField("String", "SIGN_OUT_REDIRECT_URI", "\"" + oktaProperties["signOutRedirectUri"] + "\"")
        buildConfigField("String", "OKTA_STATE", "\"" + oktaProperties["oktaState"] + "\"")
        buildConfigField("String", "SCOPES", "\"" + oktaProperties["scopes"] + "\"")
        buildConfigField("String", "CODE_CHALLENGE", "\"" + oktaProperties["codeChallenge"] + "\"")
        buildConfigField("String", "CODE_CHALLENGE_METHOD", "\"" + oktaProperties["codeChallengeMethod"] + "\"")
        buildConfigField("String", "CODE_VERIFIER", "\"" + oktaProperties["codeVerifier"] + "\"")
    }

    buildTypes {
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    viewBinding {
        enable = true
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true
    }
    lint {
        checkReleaseBuilds = true
    }
    bundle {
        language {
            enableSplit = false
        }
        density {
            enableSplit = false
        }
        abi {
            enableSplit = false
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget("17"))
    }
}

dependencies {

    //region Genesys
    if (useArtifactory) {
        val depGroupId = if (useMavenCentral) "cloud.genesys" else "com.genesys.cloud"
        implementation("$depGroupId:core:$projectVersion")
        implementation("$depGroupId:chatintegration:$projectVersion")
        implementation("$depGroupId:ui:$projectVersion")
    } else {
        // Local development: use source code from SDK modules
        implementation(project(":core"))
        implementation(project(":chatintegration"))
        implementation(project(":ui"))
    }

    //region android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material.base)
    implementation(libs.constraintlayout.app)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    //endregion

    //region Compose
    implementation(platform("androidx.compose:compose-bom:2025.11.01")) // latest version with minSdk 21 support
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    //endregion

    //region lifecycle
    implementation(sampleLibs.androidx.lifecycle.livedata.ktx)
    implementation(sampleLibs.androidx.lifecycle.viewmodel.ktx)
    //endregion

    //region navigation
    implementation(sampleLibs.androidx.navigation.fragment.ktx)
    implementation(sampleLibs.androidx.navigation.ui.ktx)
    //endregion

    //region Gson
    implementation(sampleLibs.gson.sample)
    implementation(sampleLibs.androidx.legacy.support.v4)
    //endregion

    //region Firebase
    if (isGoogleServicesFileExists) {
        implementation(sampleLibs.firebase.crashlytics.ktx)
        implementation(sampleLibs.firebase.analytics.ktx)
    }
    implementation(sampleLibs.firebase.messaging)
    //endregion

    //region Tests
    testImplementation(libs.junit.common)
    testImplementation(libs.mockk.core)
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(sampleLibs.test.junit.sample)
    androidTestImplementation(sampleLibs.espresso.sample)
    //endregion
}
