plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	kotlin("plugin.jpa") version "1.9.25"
	id("org.springframework.boot") version "3.5.16"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "team.cklob"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-data-redis")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-validation")

	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

	implementation("org.hibernate.orm:hibernate-spatial")
	implementation("org.locationtech.jts:jts-core:1.20.0")
	runtimeOnly("org.postgresql:postgresql")

	implementation("org.flywaydb:flyway-core")
	implementation("org.flywaydb:flyway-database-postgresql")

	implementation("io.jsonwebtoken:jjwt-api:0.13.0")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")

	implementation("org.bouncycastle:bcprov-jdk18on:1.84")

	implementation(platform("io.awspring.cloud:spring-cloud-aws-dependencies:3.4.2"))
	implementation("io.awspring.cloud:spring-cloud-aws-starter-s3")

	implementation("com.google.firebase:firebase-admin:9.10.0")

	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.17")

	developmentOnly("org.springframework.boot:spring-boot-docker-compose")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.springframework.security:spring-security-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("io.mockk:mockk:1.14.11")
	testImplementation("com.ninja-squad:springmockk:4.0.2")
	testImplementation("org.testcontainers:junit-jupiter")
	testImplementation("org.testcontainers:postgresql")
	testImplementation("com.redis:testcontainers-redis:2.2.4")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		// Without this, a Kotlin interface method with a body (e.g. MemberRepository's convenience
		// overload of searchSelectableByNickname) compiles to a synthetic DefaultImpls dispatch instead of
		// a real JVM `default` method. Spring Data's repository proxy only recognizes true JVM default
		// methods via Method.isDefault() -- otherwise it treats the method as yet another abstract query
		// method and tries (and fails) to derive a query from its name. This flag makes Kotlin emit real
		// default methods so Spring Data dispatches to the actual Kotlin-written body instead.
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xjvm-default=all")
	}
}

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
	val envFile = file(".env")

	if (envFile.exists()) {
		envFile.readLines()
			.map(String::trim)
			.filter { it.isNotEmpty() && !it.startsWith("#") }
			.forEach { line ->
				val parts = line.split("=", limit = 2)
				if (parts.size == 2) {
					environment(parts[0].trim(), parts[1].trim())
				}
			}
	}
}
