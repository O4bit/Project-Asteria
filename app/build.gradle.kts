import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.aboutlibraries.plugin)
    alias(libs.plugins.ksp.plugin)
}


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

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("keystore.properties")
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { stream ->
        keystoreProperties.load(stream)
    }
}

extensions.configure<ApplicationExtension> {
    namespace = "space.o4bit.projectasteria"
    compileSdk = 36

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    defaultConfig {
        applicationId = "space.o4bit.projectasteria.foss"
        minSdk = 31
        targetSdk = 36
        versionCode = 49
        versionName = "4.4.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        vectorDrawables {
            useSupportLibrary = true
        }

        experimentalProperties["android.experimental.disable-baseline-profile"] = true
        buildConfigField(
            "String",
            "ASTERIA_API_BASE_URL",
            "\"https://asteria.o4bit.dev/\""
        )

    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            signingConfig = if (keystorePropertiesFile.exists()) {
                signingConfigs.getByName("release")
            } else {
                null
            }

            vcsInfo.include = false

            packaging {
                resources {
                    excludes += "assets/dexopt/*"
                    excludes += "META-INF/*.version"
                    excludes += "META-INF/version-control-info.textproto"
                    excludes += "META-INF/com.android.tools.r8.metadata"
                }
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
        baseline = file("lint-baseline.xml")
        disable += listOf(
            "ObsoleteLintCustomCheck",
            "InvalidPackage",
            "GradleDependency"
        )
        ignoreTestSources = true
    }
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
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

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.moshi)

    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)

    implementation(libs.moshi.core)
    implementation(libs.moshi.kotlin)

    implementation(libs.datastore.preferences)
    implementation(libs.datastore.preferences.core)

    implementation(libs.work.runtime.ktx)

    implementation(libs.coil.compose)
    implementation(libs.coil.core)

    implementation(libs.lucide.icons)

    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.accompanist.permissions)

    implementation(libs.lottie.compose)

    implementation(libs.material.dialogs.core)

    implementation(libs.compose.destinations.core)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    implementation(libs.aboutlibraries.compose)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}

androidComponents {
}
