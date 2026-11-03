// JMH 基准模块：仅用于本地性能验证，不随正式产物发布
dependencies {
    implementation(project(":analyser"))
    implementation(project(":reflex"))
    implementation("org.openjdk.jmh:jmh-core:1.37")
    // JMH 注解处理器：编译期生成基准清单（BenchmarkList）与运行时桩
    annotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.37")
}

// benchmark 模块不发布到 maven 仓库
tasks.matching { it.name.startsWith("publish") || it.name.startsWith("generatePom") || it.name.startsWith("generateMetadata") }.configureEach {
    enabled = false
}

// 运行入口：./gradlew :benchmark:jmh
// 透传 JMH 参数示例：./gradlew :benchmark:jmh -PjmhArgs="-wi 5 -i 5 -f 2 ReflexInvokeBench.reflexAsm"
val jmh by tasks.registering(JavaExec::class) {
    group = "benchmark"
    description = "运行 JMH 基准测试（-PjmhArgs=\"...\" 可透传 JMH 参数）"
    mainClass.set("org.openjdk.jmh.Main")
    classpath(sourceSets["main"].runtimeClasspath)
    jvmArgs("-Dfile.encoding=UTF-8")
    if (project.hasProperty("jmhArgs")) {
        args((project.property("jmhArgs") as String).split(" "))
    }
}
