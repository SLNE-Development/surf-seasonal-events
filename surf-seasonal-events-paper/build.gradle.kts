import dev.slne.surf.microservice.gradle.plugin.rabbit.RabbitModule

plugins {
    id("dev.slne.surf.api.gradle.paper-plugin")
    id("dev.slne.surf.microservice")
}

surfMicroservice {
    withRabbitModule(RabbitModule.CLIENT_API)
}

surfPaperPluginApi {
    mainClass("dev.slne.surf.seasonalevents.paper.PaperMain")
    foliaSupported(true)

    authors.addAll(listOf("Jo_field", "Timonso"))
}

dependencies {
    api(projects.surfSeasonalEventsCore.surfSeasonalEventsCoreClient)
}
