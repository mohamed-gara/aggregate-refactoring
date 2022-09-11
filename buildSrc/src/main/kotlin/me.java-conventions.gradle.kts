plugins {
  id("java")
  kotlin("jvm")
}

group = "com.example"
version = "1.0"

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
  implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.7.10"))
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.10")

  implementation("com.google.guava:guava:31.1-jre")
  implementation("ch.qos.logback:logback-classic:1.2.11")
  implementation("org.slf4j:slf4j-api:1.7.36")

  testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")
  testImplementation("org.assertj:assertj-core:3.23.1")
}
