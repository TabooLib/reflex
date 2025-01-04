dependencies {
    implementation(project(":analyser"))
    testImplementation("org.ow2.asm:asm:9.2")
    testImplementation("org.ow2.asm:asm-util:9.2")
    testImplementation("org.ow2.asm:asm-commons:9.2")
    testImplementation(kotlin("test"))
}