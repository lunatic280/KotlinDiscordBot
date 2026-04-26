plugins {
	kotlin("jvm") version "2.2.21"
	kotlin("plugin.spring") version "2.2.21"
	id("org.springframework.boot") version "4.0.0-SNAPSHOT"
	id("io.spring.dependency-management") version "1.1.7"
	kotlin("plugin.jpa") version "2.2.21"
}

group = "com.DiscordBot"
version = "0.0.1-SNAPSHOT"
description = "Kotlin Discord Bot"

val libdaveVersion = "ce725965e"
val youtubeSourceVersion = "1.18.0"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
	maven { url = uri("https://repo.spring.io/snapshot") }
	maven { url = uri("https://maven.lavalink.dev/snapshots") }
	maven { url = uri("https://maven.lavalink.dev/releases") }
}

dependencies {
	implementation("net.dv8tion:JDA:6.4.1")

	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.springframework.boot:spring-boot-starter-web")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	runtimeOnly("org.postgresql:postgresql")
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.mockito:mockito-core:5.20.0")
	testRuntimeOnly("com.h2database:h2")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.json:json:20240303")
	implementation("org.jsoup:jsoup:1.17.2")
	implementation("org.apache.poi:poi-ooxml:5.2.5")
	implementation("dev.arbjerg:lavaplayer:2.2.6")
	implementation("dev.lavalink.youtube:v2:$youtubeSourceVersion")
	implementation("moe.kyokobot.libdave:adapter-jda:$libdaveVersion")
	implementation("moe.kyokobot.libdave:impl-jni:$libdaveVersion")
	runtimeOnly("moe.kyokobot.libdave:natives-linux-x86-64:$libdaveVersion")
	runtimeOnly("moe.kyokobot.libdave:natives-win-x86-64:$libdaveVersion")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.8.0")
	implementation("com.google.cloud:google-cloud-speech:4.39.0")

	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

tasks.named<Test>("test") {
	useJUnitPlatform {
		excludeTags("live")
	}
}

tasks.register<Test>("liveTest") {
	group = "verification"
	description = "Runs live integration tests (tag: live)."
	testClassesDirs = sourceSets["test"].output.classesDirs
	classpath = sourceSets["test"].runtimeClasspath
	useJUnitPlatform {
		includeTags("live")
	}
	testLogging {
		showStandardStreams = true
		events("passed", "failed", "skipped")
	}
}




