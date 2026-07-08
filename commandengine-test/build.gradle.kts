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
  testImplementation(platform(libs.junit.bom))
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.assertj)
  compileOnly(libs.jetbrainsAnnotations)
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
