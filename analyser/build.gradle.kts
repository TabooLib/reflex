dependencies {
    implementation("org.ow2.asm:asm:9.6")
    implementation("org.ow2.asm:asm-util:9.6")
    implementation("org.ow2.asm:asm-commons:9.6")
    implementation("org.apache.commons:commons-lang3:3.5")
    testImplementation("org.apache.commons:commons-lang3:3.5")
    testImplementation(kotlin("stdlib"))
    testImplementation(kotlin("reflect"))
}

shrinking {
    annotation = "org.tabooproject.reflex.Internal"
}