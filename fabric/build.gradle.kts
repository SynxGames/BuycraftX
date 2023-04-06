import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("fabric-loom")
    id("java")
}

version = "1.0.0-SNAPSHOT"
group = "net.buycraft"

repositories {
    mavenLocal()
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    mavenCentral()
}


dependencies {
    minecraft("com.mojang:minecraft:1.19.2")
    mappings(loom.officialMojangMappings())

    modImplementation("net.fabricmc:fabric-loader:0.14.9")
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.59.0+1.19.2")
    modImplementation("me.lucko:fabric-permissions-api:0.2-SNAPSHOT")

    implementation(project(":plugin-shared"))

    include(implementation("com.squareup.retrofit2:retrofit:2.9.0")!!)
    include(implementation("com.squareup.retrofit2:converter-gson:2.9.0")!!)
    include("com.squareup.okhttp3:okhttp:3.14.9")
    include("com.squareup.okio:okio:1.17.2")
    //include(project(":plugin-shared"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

configurations {
    shadow
}

tasks {
    remapJar {
        dependsOn(shadowJar)
        inputFile.set(shadowJar.get().archiveFile)
        archiveBaseName.set("BuycraftX-Fabric-Remapped")
    }

    shadowJar {
        //finalizedBy(remapJar)
        //dependsOn(remapJar)

        dependencies {
            include(project(":plugin-shared"))
            include(project(":common"))
        }

    }
}