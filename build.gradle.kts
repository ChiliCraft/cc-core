// cc-core：核心插件：档案 / 全局模式 / 参数服务 / 事件总线 / 灵魂货币 / DB / 统一入口
plugins {
    id("java-library")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("maven-publish")
}

group = "com.chilicraft"
version = "1.0.0"

// 统一工具链：Java 21 编译。不用 JDK 17 的原因见 monorepo 根构建脚本注释：
// 中文 Windows（GBK）下 JDK 17 worker 按平台编码读 UTF-8 argfile 会崩，JDK 18+ 默认 UTF-8。
java {
    toolchain { languageVersion = JavaLanguageVersion.of(21) }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 21
    options.compilerArgs.add("-Xlint:deprecation")
}

tasks.withType<Javadoc>().configureEach {
    options.encoding = "UTF-8"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") { name = "papermc" }
    maven("https://repo.extendedclip.com/releases/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    compileOnly("org.slf4j:slf4j-api:2.0.13")
    // 随 jar 交付的运行时依赖
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.xerial:sqlite-jdbc:3.46.1.3")
    // PlaceholderAPI：占位符变量暴露（运行时缺失由 softdepend 降级）
    compileOnly("me.clip:placeholderapi:2.12.3")
}

// 原始 jar 标记为 plain，shadowJar 产出最终构件
tasks.jar { archiveClassifier = "plain" }

tasks.shadowJar {
    archiveClassifier = ""
    archiveBaseName = "cc-core"
    // Shadow 8.1.1 定位器不能读 Java 21 字节码，不 relocate；
    // Paper 类加载器隔离 HikariCP，sqlite-jdbc 含 JNI 同样禁止 relocate。
}

tasks.build { dependsOn(tasks.shadowJar) }

// 发布 cc-core 到 GitHub Packages / mavenLocal，供各附属模块 compileOnly 解析
publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "cc-core"
            artifact(tasks.shadowJar)
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/ChiliCraft/cc-core")
            credentials {
                username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
                password = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

// 把版本号注入 plugin.yml（${version} 占位）
tasks.processResources {
    val props = mapOf("version" to project.version.toString())
    inputs.properties(props)
    filesMatching("plugin.yml") {
        expand(props)
    }
}
