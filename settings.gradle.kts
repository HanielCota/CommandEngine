rootProject.name = "commandengine"

pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
  }
}

dependencyResolutionManagement {
  defaultLibrariesExtensionName = "defaultLibs"
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
  }
  versionCatalogs {
    create("libs") {
      from(files("gradle/libs.versions.toml"))
    }
  }
}

include(
    "commandengine-api",
    "commandengine-processor",
    "commandengine-runtime",
    "commandengine-platform-paper",
    "commandengine-test",
    "commandengine-example-paper",
)
