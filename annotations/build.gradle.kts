plugins {
    id("java-library")
}

group = "sk.tuke.meta.persistence"
version = "1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

tasks.test {
    useJUnitPlatform()
}