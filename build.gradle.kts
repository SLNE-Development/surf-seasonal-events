plugins {
    id("dev.slne.surf.surfapi.gradle.paper-plugin") version "+"
}

group = "dev.slne.surf.gift"
version = findProperty("version") as String


surfPaperPluginApi {
    mainClass("dev.slne.surf.gift.PaperMain")
    generateLibraryLoader(false)
    foliaSupported(true)

    authors.add("Jo_field", "Timonso")

    withSurfDatabaseR2dbc("2.3.5", "dev.slne.surf.gift.libs")
}

