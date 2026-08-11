package com.istidblip.pengehubben

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() {
    println("NOTE: If the app crashes with 'Failed to create DirectX12 device' or shows a blank screen,")
    println("you can force software rendering by passing this JVM argument: -Dskiko.renderApi=SOFTWARE")

    try {
        val skikoVersion = Class.forName("org.jetbrains.skiko.SkikoVersion")
            .getDeclaredField("CURRENT")
            .get(null)
        println("Skiko version: $skikoVersion")
    } catch (_: Exception) {
        // Silently fail if Skiko version info is unavailable
    }

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Pengehubben",
        ) {
            App()
        }
    }
}
