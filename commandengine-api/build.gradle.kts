plugins {
  `java-library`
}

dependencies {
  compileOnly(libs.jetbrainsAnnotations)
  api(libs.brigadier)

  testImplementation(platform(libs.junit.bom))
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.assertj)
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
