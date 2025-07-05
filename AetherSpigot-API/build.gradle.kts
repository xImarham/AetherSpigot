plugins {
    id("aetherspigot.conventions")
    `maven-publish`
}

java {
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    api("commons-lang:commons-lang:2.6")
    api("org.avaje:ebean:2.8.1")
    api("com.googlecode.json-simple:json-simple:1.1.1")
    api("org.yaml:snakeyaml:1.15")
    api("net.md-5:bungeecord-chat:1.8-SNAPSHOT")
    compileOnlyApi("net.sf.trove4j:trove4j:3.0.3") // provided by server

    // bundled with Minecraft, should be kept in sync
    api("com.google.guava:guava:17.0")
    api("com.google.code.gson:gson:2.2.4")
    api("org.slf4j:slf4j-api:1.7.35") // PandaSpigot - Add SLF4J Logger
}

tasks {
    val generateApiVersioningFile by registering {
        inputs.property("version", project.version)
        val pomProps = layout.buildDirectory.file("pom.properties")
        outputs.file(pomProps)
        doLast {
            pomProps.get().asFile.writeText("version=${project.version}")
        }
    }

    jar {
        from(generateApiVersioningFile.map { it.outputs.files.singleFile }) {
            into("META-INF/maven/${project.group}/${project.name.lowercase()}")
        }

        manifest {
            attributes(
                "Automatic-Module-Name" to "org.bukkit"
            )
        }
    }

    withType<Javadoc> {
        (options as StandardJavadocDocletOptions).let {
            // hide warnings
            it.addBooleanOption("Xdoclint:none", true)
            it.addStringOption("Xmaxwarns", "1")

            it.links(
                "https://guava.dev/releases/17.0/api/docs/",
                "https://javadoc.io/doc/org.yaml/snakeyaml/1.15/",
                "https://javadoc.io/doc/net.md-5/bungeecord-chat/1.16-R0.4/",
            )
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
            }

            pom {
                url.set("https://github.com/hpfxd/PandaSpigot")
                description.set(project.description)
                name.set(project.name)
                // if this is a CI build, set version as the run id
                System.getenv("GITHUB_RUN_NUMBER").let { if (it != null) version = it }

                developers {
                    developer {
                        id.set("hpfxd")
                        name.set("Nate")
                        email.set("me@hpfxd.com")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/hpfxd/PandaSpigot.git")
                    developerConnection.set("scm:git:git://github.com/hpfxd/PandaSpigot.git")
                    url.set("https://github.com/hpfxd/PandaSpigot")
                }

                licenses {
                    license {
                        name.set("GPL-v3.0")
                        url.set("https://www.gnu.org/licenses/gpl-3.0.txt")
                    }
                }
            }
        }
    }

    (System.getenv("REPO_USERNAME") ?: findProperty("repository.hpfxd.username")).let { repoUsername ->
        if (repoUsername == null) return@let // don't declare repository if username not declared

        repositories {
            maven {
                name = "hpfxd-repo"
                url = uri("https://repo.hpfxd.com/releases/")

                credentials {
                    username = repoUsername as String
                    password = System.getenv("REPO_PASSWORD") ?: findProperty("repository.hpfxd.password") as String
                }
            }
        }
    }
}
