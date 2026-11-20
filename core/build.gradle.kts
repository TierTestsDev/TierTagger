plugins {
    `java-library`
}

repositories {
    mavenCentral()
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

dependencies {
    compileOnly("com.google.code.gson:gson:2.2.4")

    testImplementation("com.google.code.gson:gson:2.2.4")
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 8
    options.compilerArgs.add("-Xlint:-options")
}

tasks.test {
    useJUnitPlatform()
    maxParallelForks = 1
}
