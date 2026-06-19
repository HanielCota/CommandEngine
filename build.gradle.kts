import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
  java
  idea
  jacoco
  id("com.diffplug.spotless") version "8.7.0"
}

idea {
  module {
    name = "CommandEngine"
  }
}

spotless {
  format("misc") {
    target("**/*.md", "**/*.toml", "**/*.properties", "**/*.yml", "**/*.yaml")
    targetExclude("**/build/**", "**/.gradle/**")
    trimTrailingWhitespace()
    endWithNewline()
  }

  kotlinGradle {
    target("*.gradle.kts", "**/*.gradle.kts")
    targetExclude("**/build/**", "**/.gradle/**")
    ktfmt()
    trimTrailingWhitespace()
    endWithNewline()
  }
}

tasks.named("build") {
  dependsOn("spotlessCheck")
}

allprojects {
  val jitpackGroup = providers.environmentVariable("GROUP").orNull
  val jitpackArtifact = providers.environmentVariable("ARTIFACT").orNull
  group =
      if (
          providers.environmentVariable("JITPACK").isPresent &&
              jitpackGroup != null &&
              jitpackArtifact != null
      ) {
        "$jitpackGroup.$jitpackArtifact"
      } else {
        "com.hanielfialho"
      }
  version = providers.environmentVariable("VERSION").orElse("0.1.0-SNAPSHOT").get()
}

subprojects {
  apply(plugin = "java")
  apply(plugin = "idea")
  apply(plugin = "jacoco")
  apply(plugin = "com.diffplug.spotless")

  if (name != "commandengine-example-paper") {
    apply(plugin = "maven-publish")
  }

  extensions.configure<org.gradle.plugins.ide.idea.model.IdeaModel>("idea") {
    module {
      name = project.name
    }
  }

  extensions.configure<SpotlessExtension>("spotless") {
    java {
      target("src/**/*.java")
      palantirJavaFormat()
      removeUnusedImports()
      trimTrailingWhitespace()
      endWithNewline()
    }
  }

  java {
    modularity.inferModulePath.set(true)
    withSourcesJar()
    toolchain {
      languageVersion.set(JavaLanguageVersion.of(25))
    }
  }

  pluginManager.withPlugin("maven-publish") {
    extensions.configure<PublishingExtension>("publishing") {
      publications {
        create<MavenPublication>("mavenJava") {
          from(components["java"])
          artifactId = project.name
          pom {
            name.set(project.name)
            description.set("CommandEngine module ${project.name}")
            url.set("https://github.com/HanielCota/CommandEngine")
            licenses {
              license {
                name.set("MIT License")
                url.set("https://opensource.org/license/mit")
              }
            }
            developers {
              developer {
                id.set("HanielCota")
                name.set("Haniel Fialho")
              }
            }
            scm {
              connection.set("scm:git:https://github.com/HanielCota/CommandEngine.git")
              developerConnection.set("scm:git:https://github.com/HanielCota/CommandEngine.git")
              url.set("https://github.com/HanielCota/CommandEngine")
            }
          }
        }
      }
    }
  }

  tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(25)
    if (project.name == "commandengine-api") {
      options.compilerArgs.add("-Xlint:-requires-transitive-automatic")
    }
    if (name == "compileJava" && project.file("src/main/java/module-info.java").exists()) {
      doFirst {
        options.compilerArgs.addAll(listOf("--module-path", classpath.asPath))
        classpath = files()
      }
    }
  }

  tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy("jacocoTestReport")
  }

  tasks.withType<JacocoReport> {
    dependsOn("test")
    reports {
      xml.required.set(true)
      html.required.set(true)
    }
  }

  tasks.named("build") {
    dependsOn("spotlessCheck")
  }
}
