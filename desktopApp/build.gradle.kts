plugins {
    kotlin("jvm")
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(compose.desktop.windows_x64)
}

tasks.register("saveDependencies") {
    doLast {
        val outputFile = file("dependencies.txt")
        outputFile.writeText("")
        configurations.named("runtimeClasspath").get().resolvedConfiguration.lenientConfiguration.allModuleDependencies.forEach { dep ->
            outputFile.appendText("${dep.name} -> ${dep.moduleVersion}\n")
            dep.children.forEach { child ->
                outputFile.appendText("  ${child.name} -> ${child.moduleVersion}\n")
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.istidblip.pengehubben.MainKt"

        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg, org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi, org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb)
            packageName = "com.istidblip.pengehubben"
            packageVersion = "1.0.0"
        }
    }
}
