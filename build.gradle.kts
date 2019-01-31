import org.gradle.jvm.tasks.Jar

plugins {
    java
    application
}

project.group = "com.ride.android"
project.version = "1.0"
val artifactID = "ride-android"

repositories {
    jcenter()
}

dependencies {
    implementation("com.google.guava:guava:26.0-jre")
    implementation("com.linkedin.dexmaker:dexmaker:2.21.0")
    implementation("info.picocli:picocli:3.9.2")
    testImplementation("junit:junit:4.12")
}

application {
    mainClassName = "com.ride.android.MainCompiler"
}

tasks.create<Jar>("fatJar") {
    appendix = "fat"
    duplicatesStrategy = DuplicatesStrategy.FAIL
    manifest {
        attributes(mapOf("Main-Class" to "com.ride.android.MainCompiler")) // replace it with your own
    }
    val sourceMain = project.sourceSets["main"]
    from(sourceMain.output)

    configurations.runtimeClasspath.get().filter({
        it.name.endsWith(".jar")
    }).forEach { jar ->
        from(zipTree(jar))
    }
}