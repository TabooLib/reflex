plugins {
    java
    `maven-publish`
    id("org.jetbrains.kotlin.jvm") version "1.5.10" apply false
    id("org.tabooproject.shrinkingkt") version "1.0.2" apply false
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.tabooproject.shrinkingkt")

    repositories {
        mavenCentral()
    }

    dependencies {
        "implementation"(kotlin("stdlib"))
        "testRuntimeOnly"("org.junit.jupiter:junit-jupiter-engine:5.8.1")
        "testImplementation"("org.junit.jupiter:junit-jupiter-api:5.8.1")
    }

    configure<JavaPluginConvention> {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    publishing {
        repositories {
            maven("http://ptms.ink:8081/repository/releases") {
                isAllowInsecureProtocol = true
                credentials {
                    username = project.findProperty("taboolibUsername").toString()
                    password = project.findProperty("taboolibPassword").toString()
                }
                authentication {
                    create<BasicAuthentication>("basic")
                }
            }
        }
        publications {
            create<MavenPublication>("maven") {
                from(components.findByName("java"))
            }
        }
    }

    tasks {
        test {
            useJUnitPlatform()
        }
    }
}