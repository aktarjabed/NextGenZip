// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.5.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.23" apply false
    kotlin("plugin.serialization") version "1.9.23" apply false
    id("com.google.protobuf") version "0.9.4" apply false
}

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.5.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.23")
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
