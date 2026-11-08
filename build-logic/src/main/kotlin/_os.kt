@file:Suppress("ktlint:standard:filename")

enum class OS(val id: String) {
    Linux("linux"),
    Windows("windows"),
    MacOS("macos")
}

enum class Arch(val id: String) {
    X64("x64"),
    Arm64("arm64")
}

data class Target(val os: OS, val arch: Arch) {
    val id: String
        get() = "${os.id}-${arch.id}"
}
