plugins {
id("com.android.application") version "8.13.0" apply false
id("org.jetbrains.kotlin.android") version "2.2.20" apply false
id("org.jetbrains.kotlin.plugin.compose") version "2.2.20" apply false
}

dependencyResolutionManagement {
repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
repositories {
google()
mavenCentral()
}
}
