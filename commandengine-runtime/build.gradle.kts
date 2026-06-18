plugins {
  java
}

dependencies {
  implementation(project(":commandengine-api"))
  implementation(libs.caffeine)
  compileOnly(libs.brigadier)
  compileOnly(libs.jetbrainsAnnotations)
  testImplementation(libs.brigadier)
  testImplementation(platform(libs.junit.bom))
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.assertj)
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
