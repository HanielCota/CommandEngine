plugins {
  java
  id("com.gradleup.shadow") version "9.0.0-beta12"
}

dependencies {
  implementation(project(":commandengine-api"))
  implementation(project(":commandengine-runtime"))
  implementation(project(":commandengine-platform-paper"))
  compileOnly(libs.paper.api) {
    exclude(group = "org.apache.maven")
    exclude(group = "org.apache.maven.resolver")
    exclude(group = "org.codehaus.plexus")
    exclude(group = "javax.inject")
    exclude(group = "org.eclipse.sisu")
    exclude(group = "com.google.auto.service")
  }
  annotationProcessor(project(":commandengine-processor"))
}

tasks {
  named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveClassifier.set("")
  }
}
