// Latest stable toolchain as of October 2026. AGP 9 has Kotlin built in, so only the Compose compiler plugin is applied.
plugins {
    id("com.android.application") version "9.4.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
}
