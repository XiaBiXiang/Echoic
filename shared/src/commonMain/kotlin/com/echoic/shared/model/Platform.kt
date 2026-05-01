package com.echoic.shared.model

/**
 * Platform and architecture support for local TTS models.
 */
enum class Platform(
    val displayName: String,
    val os: OS,
    val arch: Architecture,
) {
    MACOS_X64("macOS x86_64", OS.MACOS, Architecture.X86_64),
    MACOS_ARM64("macOS ARM64 (Apple Silicon)", OS.MACOS, Architecture.ARM64),
    WINDOWS_X64("Windows x86_64", OS.WINDOWS, Architecture.X86_64),
    WINDOWS_ARM64("Windows ARM64", OS.WINDOWS, Architecture.ARM64),
    LINUX_X64("Linux x86_64", OS.LINUX, Architecture.X86_64),
    LINUX_ARM64("Linux ARM64", OS.LINUX, Architecture.ARM64),
    LINUX_ARMV7("Linux ARMv7", OS.LINUX, Architecture.ARMV7),
    ANDROID_ARM64("Android ARM64", OS.ANDROID, Architecture.ARM64),
    ANDROID_ARMV7("Android ARMv7", OS.ANDROID, Architecture.ARMV7),
    IOS_ARM64("iOS ARM64", OS.IOS, Architecture.ARM64),
    WEB_WASM("WebAssembly", OS.WEB, Architecture.WASM),
    ;

    enum class OS(val displayName: String) {
        MACOS("macOS"),
        WINDOWS("Windows"),
        LINUX("Linux"),
        ANDROID("Android"),
        IOS("iOS"),
        WEB("Web"),
    }

    enum class Architecture(val displayName: String) {
        X86_64("x86_64"),
        ARM64("ARM64"),
        ARMV7("ARMv7"),
        WASM("WebAssembly"),
    }

    companion object {
        /** Get all desktop platforms (macOS, Windows, Linux). */
        val desktop: List<Platform> = entries.filter {
            it.os in listOf(OS.MACOS, OS.WINDOWS, OS.LINUX)
        }

        /** Get all mobile platforms. */
        val mobile: List<Platform> = entries.filter {
            it.os in listOf(OS.ANDROID, OS.IOS)
        }

        /** Get platforms for a specific OS. */
        fun forOS(os: OS): List<Platform> = entries.filter { it.os == os }

        /** Get platforms for a specific architecture. */
        fun forArch(arch: Architecture): List<Platform> = entries.filter { it.arch == arch }
    }
}
