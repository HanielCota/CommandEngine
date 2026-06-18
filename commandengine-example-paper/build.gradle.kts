plugins {
  java
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
