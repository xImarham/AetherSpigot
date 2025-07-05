rootProject.name = "aetherspigot"

includeBuild("build-logic")

this.setupSubproject("aetherspigot-server", "AetherSpigot-Server")
this.setupSubproject("aetherspigot-api", "AetherSpigot-API")

fun setupSubproject(name: String, dir: String) {
    include(":$name")
    project(":$name").projectDir = file(dir)
}
