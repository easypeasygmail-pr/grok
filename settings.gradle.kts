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

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Odpisz"

include(
    ":app",
    ":core:model",
    ":core:privacy",
    ":core:network",
    ":core:database",
    ":core:ocr",
    ":core:pdf",
    ":core:data",
    ":core:designsystem",
    ":feature:onboarding",
    ":feature:scan",
    ":feature:review",
    ":feature:letter",
    ":feature:reply",
    ":feature:redcard",
    ":feature:history",
    ":feature:settings",
)
