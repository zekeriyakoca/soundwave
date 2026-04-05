plugins {
	java
	id("org.springframework.boot") version "4.0.5"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.soundwave"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-data-redis")
	implementation("org.springframework.boot:spring-boot-starter-flyway")
	implementation("org.springframework.boot:spring-boot-starter-kafka")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.flywaydb:flyway-mysql")
	compileOnly("org.projectlombok:lombok")
	runtimeOnly("org.mariadb.jdbc:mariadb-java-client")
	annotationProcessor("org.projectlombok:lombok")

	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.8")
	implementation("net.logstash.logback:logstash-logback-encoder:7.4")
	implementation("io.micrometer:micrometer-registry-prometheus")

	// Testing
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.testcontainers:junit-jupiter:1.21.0")
	testImplementation("org.testcontainers:mariadb:1.21.0")
	testCompileOnly("org.projectlombok:lombok")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testAnnotationProcessor("org.projectlombok:lombok")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

val unitTest by tasks.registering(Test::class) {
	group = "verification"
	description = "Runs unit tests (excluding IntegrationTest classes)."
	val testSourceSet = sourceSets["test"]
	testClassesDirs = testSourceSet.output.classesDirs
	classpath = testSourceSet.runtimeClasspath
	useJUnitPlatform()
	include("**/*Test.class")
	exclude("**/*IntegrationTest.class")
}

val integrationTest by tasks.registering(Test::class) {
	group = "verification"
	description = "Runs integration tests (*IntegrationTest)."
	val testSourceSet = sourceSets["test"]
	testClassesDirs = testSourceSet.output.classesDirs
	classpath = testSourceSet.runtimeClasspath
	useJUnitPlatform()
	include("**/*IntegrationTest.class")
	shouldRunAfter(unitTest)
}
