import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

plugins {
    id("java-library")
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
    // 统一的大模型适配层（openai / anthropic / dashscope / gemini 及网关）。
    // 核心公开 API（ChatEngine / AdapterManager）直接暴露 converter 类型，
    // 必须用 api 让消费者（如 Android App）在编译期可见。
    api("org.bluepowerrobotics:AIAPIConverter:1.0-SNAPSHOT")

    // JSON 处理（converter 的 api 依赖会透传，这里显式声明避免隐式依赖）
    implementation("com.fasterxml.jackson.core:jackson-databind:2.19.4")

    // 网页抓取工具（FetchUrl）
    implementation("org.htmlunit:htmlunit:3.11.0")

    // 运行时不需要具体日志实现，避免 SLF4J 警告
    runtimeOnly("org.slf4j:slf4j-nop:2.0.18")

    implementation("com.ibm.icu:icu4j:78.3")

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

// 打包为自包含 CLI：jlink 裁剪 JVM 模块 + bin/ask-cli 可执行启动器。
// 产物结构：build/askcli/{bin/ask-cli, lib/AskCLI.jar, runtime/}
tasks.register("jlinkApp") {
    dependsOn("fatJar")
    doLast {
        val jarFile = tasks.named<Jar>("fatJar").get().archiveFile.get().asFile
        val appDir = layout.buildDirectory.dir("askcli").get().asFile
        val libDir = File(appDir, "lib")
        val binDir = File(appDir, "bin")
        val runtimeDir = File(appDir, "runtime")
        libDir.mkdirs()
        binDir.mkdirs()

        fun run(vararg cmd: String) {
            val process = ProcessBuilder(*cmd)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            val code = process.waitFor()
            if (code != 0) {
                throw GradleException(
                    "命令 ${cmd[0]} 失败 (exit $code): ${output.takeLast(2000)}")
            }
        }

        // 1) 用 jdeps 计算 fat jar 依赖的 JVM 模块
        val moduleOutput = ByteArrayOutputStream()
        val jdeps = ProcessBuilder("jdeps", "--ignore-missing-deps",
            "--print-module-deps", jarFile.absolutePath)
            .redirectErrorStream(true)
            .start()
        moduleOutput.write(jdeps.inputStream.readBytes())
        if (jdeps.waitFor() != 0) {
            throw GradleException("jdeps 失败: ${moduleOutput.toString("UTF-8").takeLast(2000)}")
        }
        val modules = moduleOutput.toString("UTF-8").trim()
        if (modules.isEmpty()) {
            throw GradleException("jdeps 未计算出模块依赖")
        }
        logger.lifecycle("jlink modules: $modules")

        // 2) 只放 fat jar 进 lib/
        Files.copy(
            jarFile.toPath(),
            Paths.get(libDir.absolutePath, jarFile.name),
            StandardCopyOption.REPLACE_EXISTING)

        // 3) jlink 生成裁剪 runtime（jlink 不允许输出目录已存在，先删旧目录）
        project.delete(runtimeDir)
        run("jlink",
            "--add-modules", modules,
            "--output", runtimeDir.absolutePath,
            "--strip-debug",
            "--compress", "zip-6",
            "--no-header-files",
            "--no-man-pages")

        // 4) 生成 App CDS 存档，缓存启动期加载的类（用系统 JDK 生成，
        //    与 jlink runtime 同为当前 JDK，版本兼容）；失败不阻断打包
        val cdsArchive = File(libDir, "askcli.jsa")
        try {
            run("java",
                "-XX:ArchiveClassesAtExit=${cdsArchive.absolutePath}",
                "-jar", jarFile.absolutePath,
                "--help")
        } catch (e: Exception) {
            logger.warn("App CDS 存档生成失败（不影响运行）: ${e.message}")
        }

        // 5) 写启动器：相对定位 runtime 与 jar，可整体拷贝到任何位置
        val launcher = File(binDir, "ask-cli")
        val script = "#!/bin/sh\n" +
                "DIR=\"\$(cd \"\$(dirname \"\$0\")\" && pwd)\"\n" +
                "exec \"\$DIR/../runtime/bin/java\" " +
                "-XX:TieredStopAtLevel=1 -Xss2m " +
                "-XX:SharedArchiveFile=\"\$DIR/../lib/askcli.jsa\" " +
                "-jar \"\$DIR/../lib/${jarFile.name}\" \"\$@\"\n"
        Files.write(launcher.toPath(),
            script.toByteArray(Charsets.UTF_8))
        launcher.setExecutable(true, false)

        logger.lifecycle("自包含 CLI 已生成: ${launcher.absolutePath}")
    }
}
