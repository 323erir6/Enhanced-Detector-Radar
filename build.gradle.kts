plugins {
    java
    id("net.neoforged.moddev") version "2.0.80"
}

version = property("mod_version")!!
group = "dev.radarcompat.enhanceddetector"

base {
    archivesName.set("enhanced-detector-radar")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
}

neoForge {
    version = property("neo_version") as String
    mods {
        create("enhanced_detector_radar") {
            sourceSet(sourceSets.main.get())
        }
    }
}

dependencies {
    compileOnly(files("libs/AdvancedPeripherals-1.21.1-0.7.61b.jar"))
    compileOnly(files("libs/neoperipheral-1.4ve.jar"))
    compileOnly(files("libs/sable-neoforge-1.21.1-2.0.5.jar"))
    compileOnly(files("libs/sable-companion-common-1.21.1-1.6.0.jar"))
    compileOnly(files("libs/cc-tweaked-1.21.1-forge-1.120.0.jar"))
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand("version" to project.version)
    }
}
