plugins {
  java
  `java-library`
}

dependencies {
  implementation(project(":commandengine-api"))
  implementation(project(":commandengine-runtime"))
  implementation(libs.brigadier)
  annotationProcessor(project(":commandengine-processor"))
  testAnnotationProcessor(project(":commandengine-processor"))
  api(platform(libs.junit.bom))
  api(libs.junit.jupiter)
  api(libs.assertj)
  compileOnly(libs.jetbrainsAnnotations)
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
