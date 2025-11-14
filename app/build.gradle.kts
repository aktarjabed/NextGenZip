plugins {
id("com.android.application")
id("org.jetbrains.kotlin.android")
id("org.jetbrains.kotlin.plugin.compose")
}

android {
namespace = "com.aktarjabed.nextgenzip"
compileSdk = 35

defaultConfig {
applicationId = "com.aktarjabed.nextgenzip"
minSdk = 32
targetSdk = 35
versionCode = 1
versionName = "0.1.0"

ndk {
abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
}

externalNativeBuild {
cmake {
cppFlags("")
}
}
}

buildTypes {
release {
isMinifyEnabled = true
proguardFiles(
getDefaultProguardFile("proguard-android-optimize.txt"),
"proguard-rules.pro"
)
}
debug {
isMinifyEnabled = false
}
}

compileOptions {
sourceCompatibility = JavaVersion.VERSION_11
targetCompatibility = JavaVersion.VERSION_11
}

kotlinOptions {
jvmTarget = "11"
}

externalNativeBuild {
cmake {
path = file("src/main/cpp/CMakeLists.txt")
version = "3.22.1"
}
}

packaging {
jniLibs {
useLegacyPackaging = false
}
resources {
excludes += setOf("META-INF/DEPENDENCIES", "META-INF/LICENSE")
}
}

buildFeatures {
compose = true
}

kotlin {
jvmToolchain(11)
}
}

repositories {
google()
mavenCentral()
}

dependencies {
// Kotlin
implementation("org.jetbrains.kotlin:kotlin-stdlib:2.2.20")

// Compose BOM (Latest stable: August 2025)
val composeBom = platform("androidx.compose:compose-bom:2025.08.00")
implementation(composeBom)
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.ui:ui-tooling-preview")
implementation("androidx.compose.material3:material3")
implementation("androidx.activity:activity-compose:1.9.3")

// AndroidX Core + Lifecycle
implementation("androidx.core:core-ktx:1.15.0")
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

// WorkManager
implementation("androidx.work:work-runtime-ktx:2.10.0")

// Archive libraries
implementation("net.lingala.zip4j:zip4j:2.11.5")
implementation("org.apache.commons:commons-compress:1.26.0")
implementation("com.github.junrar:junrar:7.5.4")

// TensorFlow Lite (Stable - fallback AI)
implementation("org.tensorflow:tensorflow-lite:2.14.0")
implementation("org.tensorflow:tensorflow-lite-support:0.4.4")

// Debug tooling
debugImplementation("androidx.compose.ui:ui-tooling")
debugImplementation("androidx.compose.ui:ui-test-manifest")
}
