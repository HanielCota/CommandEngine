import com.diffplug.gradle.spotless.SpotlessExtension
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
  group = "com.hanielfialho"
  version = "0.1.0-SNAPSHOT"
}

subprojects {
  apply(plugin = "java")
  apply(plugin = "idea")
  apply(plugin = "jacoco")
  apply(plugin = "com.diffplug.spotless")

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
    toolchain {
      languageVersion.set(JavaLanguageVersion.of(25))
    }
  }

  tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(25)
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
