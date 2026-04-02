// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

buildscript {
    dependencies {
        if (gradle.startParameter.taskNames.any { it.contains("play", ignoreCase = true) }) {
            classpath("com.google.gms:google-services:4.4.2")
            classpath("com.google.firebase:firebase-crashlytics-gradle:3.0.2")
        }
    }
}