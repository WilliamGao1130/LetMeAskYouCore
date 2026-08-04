plugins {
    id("java")
}

group = "org.bluepowerrobotics"
version = "1.0-SNAPSHOT"

java {
    // 与 AIAPIConverter 保持一致，方便以后嵌入 Android/其他 JVM 项目
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    mavenCentral()
}

dependencies {
    // 统一的大模型适配层（openai / anthropic / dashscope / gemini 及网关）
    implementation("org.bluepowerrobotics:AIAPIConverter:1.0-SNAPSHOT")

    // JSON 处理（converter 的 api 依赖会透传，这里显式声明避免隐式依赖）
    implementation("com.fasterxml.jackson.core:jackson-databind:2.19.4")

    // 网页抓取工具（FetchUrl）
    implementation("org.htmlunit:htmlunit:3.11.0")

    // 运行时不需要具体日志实现，避免 SLF4J 警告
    runtimeOnly("org.slf4j:slf4j-nop:2.0.18")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

// 一键打包可执行 fat jar（含 AIAPIConverter 与全部依赖），主类是 CLI
tasks.register<Jar>("fatJar") {
    archiveClassifier.set("all")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = "org.bluepowerrobotics.letmeaskyou.cli.AskCLI"
        attributes["Implementation-Title"] = "LetMeAskYouCore CLI"
        attributes["Implementation-Version"] = project.version
    }
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith("jar") }
            .map { zipTree(it) }
    })
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}
