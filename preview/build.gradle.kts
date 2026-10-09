// Desktop-only helper: renders the exercise figures and the muscle map to PNG files so
// they can be checked by eye without a phone. Not part of the app.
plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

val shared = "../app/src/main/java/de/meintraining/app"

sourceSets {
    main {
        kotlin.srcDirs("src", "$shared/figure", "$shared/catalog", "$shared/musclemap")
    }
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("PreviewKt")
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.layout.buildDirectory.dir("previews").get().asFile.apply { mkdirs() }
    systemProperty("java.awt.headless", "true")
}
