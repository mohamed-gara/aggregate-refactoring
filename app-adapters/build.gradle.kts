plugins {
  id("me.java-conventions")
}

dependencies {
  api(project(":app"))
  api("org.litote.kmongo:kmongo:4.7.0")

  testImplementation("org.testcontainers:mongodb:1.17.3")
  testImplementation("org.testcontainers:testcontainers:1.17.3")
}