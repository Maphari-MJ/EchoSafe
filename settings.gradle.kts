// Fix for AGP AndroidLocationsException when both ANDROID_USER_HOME and ANDROID_PREFS_ROOT are set
try {
    val processEnvClass = Class.forName("java.lang.ProcessEnvironment")
    val fields = arrayOf("theEnvironment", "theCaseInsensitiveEnvironment")
    for (fieldName in fields) {
        try {
            val field = processEnvClass.getDeclaredField(fieldName)
            field.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val map = field.get(null) as? MutableMap<String, String>
            map?.remove("ANDROID_PREFS_ROOT")
        } catch (_: Throwable) {}
    }
} catch (_: Throwable) {}

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "EchoSafe"
include(":app")
