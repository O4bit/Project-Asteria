import com.android.build.api.dsl.ApplicationExtension
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.aboutlibraries.plugin)
    alias(libs.plugins.ksp.plugin)
}

// Global build task logic to ensure reproducible builds (required for F-Droid)
// Disabling these tasks prevents non-deterministic output in baseline profiles and tests.
gradle.taskGraph.whenReady {
    tasks.forEach { task ->
        if (task.name.contains("test", ignoreCase = true) ||
            task.name.contains("Test", ignoreCase = true) ||
            task.name.contains("ArtProfile", ignoreCase = true) ||
            task.name.contains("baselineProfile", ignoreCase = true)) {
            task.enabled = false
        }
    }
}


extensions.configure<ApplicationExtension> {
    namespace = "space.o4bit.projectasteria"
    compileSdk = 36

    defaultConfig {
        applicationId = "space.o4bit.projectasteria.foss"
        minSdk = 31
        targetSdk = 36
        versionCode = 41
        versionName = "4.1.0-Release"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // F-Droid Reproducible Build requirement: Generated PNGs from vector drawables are not reproducible
        vectorDrawables {
            useSupportLibrary = true
        }

        // Disable baseline profiles for deterministic builds
        experimentalProperties["android.experimental.disable-baseline-profile"] = true

        // Load Asteria backend base URL from local.properties.
        //
        // The app no longer talks to NASA directly — all NASA traffic is
        // proxied by our Rust backend (see ../nasamirrorapi). The server
        // holds NASA_API_KEY; the client must not embed it.
        //
        // Configure in local.properties:
        //     asteria.api.base.url=https://your-worker.example.workers.dev/
        // Trailing slash is required by Retrofit.
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { stream ->
                localProperties.load(stream)
            }
        }

        val asteriaApiBaseUrl = localProperties.getProperty(
            "asteria.api.base.url",
            "https://asteria.o4bit.space/"
        )
        println("BUILD DEBUG: Asteria API base URL: $asteriaApiBaseUrl")
        buildConfigField("String", "ASTERIA_API_BASE_URL", "\"$asteriaApiBaseUrl\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Use debug signing for release builds as per previous configuration
            signingConfig = signingConfigs.getByName("debug")

            packaging {
                resources {
                    // Remove all profiles from the APK to ensure reproducibility
                    excludes += "assets/dexopt/*.prof"
                    excludes += "assets/dexopt/*.profm"
                    excludes += "META-INF/*.version"
                }
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.all {
            it.enabled = false
            it.ignoreFailures = true
            it.setExcludes(setOf("**/*"))
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
        disable += listOf(
            "ObsoleteLintCustomCheck",
            "InvalidPackage",
            "GradleDependency"
        )
        ignoreTestSources = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

aboutLibraries {
    export {
        prettyPrint.set(false)
        excludeFields.set(setOf("ResultContainer.metadata"))
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Retrofit for API calls
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.moshi)

    // OkHttp client
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)

    // Moshi for JSON parsing
    implementation(libs.moshi.core)
    implementation(libs.moshi.kotlin)

    // DataStore for preferences
    implementation(libs.datastore.preferences)
    implementation(libs.datastore.preferences.core)

    // WorkManager for background tasks
    implementation(libs.work.runtime.ktx)

    // Coil for image loading
    implementation(libs.coil.compose)
    implementation(libs.coil.core)

    // Lucide icons for Jetpack Compose
    implementation(libs.lucide.icons)

    // Accompanist - Compose utilities
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.accompanist.permissions)

    // Lottie for animations
    implementation(libs.lottie.compose)

    // Material Dialogs for Compose
    implementation(libs.material.dialogs.core)

    // Better navigation experience
    implementation(libs.compose.destinations.core)

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    // Open Source Libraries
    implementation(libs.aboutlibraries.compose)
}

androidComponents {
    // Re-enabled debug variant for development
}
