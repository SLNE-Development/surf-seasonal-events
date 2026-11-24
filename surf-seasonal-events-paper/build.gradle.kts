import dev.slne.surf.api.gradle.util.registerRequired
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

    // The core client talks to the microservice through RabbitMQ
    serverDependencies {
        registerRequired("surf-rabbitmq-paper")
    }

    authors.addAll(listOf("Jo_field", "Timonso"))
}

dependencies {
    api(projects.surfSeasonalEventsCore.surfSeasonalEventsCoreClient)
}

sourceSets.test {
    compileClasspath += sourceSets.main.get().compileClasspath
    runtimeClasspath += sourceSets.main.get().compileClasspath
}
