plugins {
  java
}

dependencies {
  implementation(project(":commandengine-api"))
  implementation(project(":commandengine-runtime"))
  implementation(libs.brigadier)
  annotationProcessor(project(":commandengine-processor"))
  testAnnotationProcessor(project(":commandengine-processor"))
  implementation(platform(libs.junit.bom))
  implementation(libs.junit.jupiter)
  implementation(libs.assertj)
  compileOnly(libs.jetbrainsAnnotations)
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
