/*
 * Gradle Kotlin JVM + CLI (단일 모듈 app)
 * - mainClass: store.MainKt
 * - 테스트: kotlin.test 사용
 */

plugins {
    alias(libs.plugins.kotlin.jvm) // 버전 카탈로그 사용 (settings/libs.versions.toml)
    application
}

repositories {
    mavenCentral()
}

dependencies {
    // kotlin.test (표준 테스트 프레임워크)
    testImplementation(kotlin("test"))

    // ── 불필요 항목 제거 ──────────────────────────────
    // implementation(libs.guava)
    // testImplementation(libs.junit.jupiter.engine)
    // testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // ─────────────────────────────────────────────────
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    // 우리의 진입점(main 함수가 있는 파일)은 store/Main.kt 이므로:
    mainClass.set("store.MainKt")
}

tasks.test {
    useJUnitPlatform()
}
