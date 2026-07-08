plugins {
  java
}

dependencies {
  implementation(project(":commandengine-api"))
  compileOnly(libs.jetbrainsAnnotations)
  testImplementation(libs.compile.testing)
  testImplementation(libs.guava)
  testImplementation(platform(libs.junit.bom))
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.assertj)
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
