plugins {
    alias(libs.plugins.jetbrains.atomic) apply false
    alias(libs.plugins.jetbrains.compose.compiler) apply false
    alias(libs.plugins.vanniktech.publish) apply false
    id("arch-ci")
}

tasks.matching {
    it.name == "kotlinWasmUpgradePackageLock" || it.name == "kotlinWasmStorePackageLock"
}.configureEach {
    dependsOn(tasks.matching { it.name == "kotlinWasmNpmInstall" })
}

tasks.matching { it.name == "kotlinWasmNpmInstall" }.configureEach {
    doLast {
        val packageLock = layout.buildDirectory.file("wasm/package-lock.json").get().asFile
        if (!packageLock.exists()) {
            packageLock.parentFile.mkdirs()
            packageLock.writeText("{}\n")
        }
    }
}

