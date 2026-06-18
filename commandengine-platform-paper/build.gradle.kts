plugins {
  java
}

dependencies {
  implementation(project(":commandengine-api"))
  implementation(project(":commandengine-runtime"))
  implementation(libs.caffeine)
  compileOnly(libs.paper.api) {
    exclude(group = "org.apache.maven")
    exclude(group = "org.apache.maven.resolver")
    exclude(group = "org.codehaus.plexus")
    exclude(group = "javax.inject")
    exclude(group = "org.eclipse.sisu")
    exclude(group = "com.google.auto.service")
  }
  compileOnly(libs.brigadier)
  compileOnly(libs.jetbrainsAnnotations)
  testImplementation(libs.brigadier)
  testImplementation(platform(libs.junit.bom))
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.assertj)
  testImplementation(libs.paper.api) {
    exclude(group = "org.apache.maven")
    exclude(group = "org.apache.maven.resolver")
    exclude(group = "org.codehaus.plexus")
    exclude(group = "javax.inject")
    exclude(group = "org.eclipse.sisu")
    exclude(group = "com.google.auto.service")
  }
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
