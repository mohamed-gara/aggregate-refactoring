plugins {
    kotlin("jvm") version "1.7.10"
}

repositories {
    mavenCentral()
}

kotlin {
  jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(17))
  }
}

tasks.test {
  useJUnitPlatform()
}

dependencies {
  implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

  implementation("com.google.guava:guava:31.1-jre")
  implementation("ch.qos.logback:logback-classic:1.2.11")
  implementation("org.slf4j:slf4j-api:1.7.36")
  implementation("org.litote.kmongo:kmongo:4.7.0")

  testImplementation("org.testcontainers:mongodb:1.17.3")
  testImplementation("org.testcontainers:testcontainers:1.17.3")
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")
  testImplementation("org.assertj:assertj-core:3.23.1")
  testImplementation("com.h2database:h2:2.1.214")
}
