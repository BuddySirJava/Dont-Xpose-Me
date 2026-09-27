pluginManagement {
    repositories {
        // Prefer Google/Maven Central for F-Droid and CI; Aliyun is a fallback
        // when dl.google.com is unreachable (e.g. sanctioned networks).
        google()
        mavenCentral()
        gradlePluginPortal()
        maven(url = "https://maven.aliyun.com/repository/google")
        maven(url = "https://maven.aliyun.com/repository/gradle-plugin")
        maven(url = "https://maven.aliyun.com/repository/public")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://maven.aliyun.com/repository/google")
        maven(url = "https://maven.aliyun.com/repository/public")
    }
}

rootProject.name = "Dont-Xpose-Me"
include(":app")
include(":xposed-api")
