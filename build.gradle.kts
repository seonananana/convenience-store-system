plugins {
    kotlin("jvm") version "2.2.0"
    application
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(24))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
}

application {
    // Main.kt의 패키지/메인 클래스 명시
    mainClass.set("store.MainKt")
}

/**
 * 실행 가능한 fat JAR 생성 (외부 라이브러리 최소화: 표준 라이브러리만 포함)
 * 빌드 산출물: build/libs/convenience-store-system-all.jar
 */
tasks.jar {
    archiveBaseName.set("convenience-store-system")
    archiveClassifier.set("all")
    manifest {
        attributes["Main-Class"] = "store.MainKt"
    }
    // 런타임 클래스패스(표준 라이브러리 포함) 병합
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith(".jar") }
            .map { zipTree(it) }
    })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
