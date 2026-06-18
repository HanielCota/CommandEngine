plugins {
  `java-library`
}

dependencies {
  compileOnly(libs.jetbrainsAnnotations)
  api(libs.brigadier)
}
